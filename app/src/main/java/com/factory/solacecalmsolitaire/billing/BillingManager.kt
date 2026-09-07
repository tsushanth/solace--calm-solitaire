package com.factory.solacecalmsolitaire.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the Play Billing connection lifecycle, product catalog, and purchase flow.
 * [PremiumManager] consumes [ownedProductIds] as the source of truth for entitlement.
 */
class BillingManager(
    context: Context,
    billingClientFactory: (Context, PurchasesUpdatedListener) -> BillingClient = { ctx, listener ->
        BillingClient.newBuilder(ctx)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
    }
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val billingClient: BillingClient = billingClientFactory(appContext, this)

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    /** Currently owned, verified, non-tip product IDs — active subscriptions plus lifetime. */
    private val _ownedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val ownedProductIds: StateFlow<Set<String>> = _ownedProductIds.asStateFlow()

    /** True once [queryExistingPurchases] has completed at least one real round-trip to Play. */
    private val _hasSyncedOnce = MutableStateFlow(false)
    val hasSyncedOnce: StateFlow<Boolean> = _hasSyncedOnce.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 4)
    val purchaseEvents: SharedFlow<PurchaseResult> = _purchaseEvents.asSharedFlow()

    private var retryAttempt = 0

    fun startConnection() {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) return
        _connectionState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                when (result.responseCode) {
                    BillingResponseCode.OK -> {
                        retryAttempt = 0
                        _connectionState.value = BillingConnectionState.CONNECTED
                        scope.launch {
                            queryProductDetails()
                            queryExistingPurchases()
                        }
                    }
                    BillingResponseCode.BILLING_UNAVAILABLE ->
                        _connectionState.value = BillingConnectionState.UNAVAILABLE
                    else ->
                        _connectionState.value = BillingConnectionState.DISCONNECTED
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.DISCONNECTED
                retryConnectionWithBackoff()
            }
        })
    }

    private fun retryConnectionWithBackoff() {
        val delayMs = minOf(30_000L, 1000L * (1L shl retryAttempt.coerceAtMost(5)))
        retryAttempt++
        scope.launch {
            delay(delayMs)
            startConnection()
        }
    }

    suspend fun queryProductDetails() {
        if (_connectionState.value != BillingConnectionState.CONNECTED) return
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingProducts.SUBSCRIPTION_IDS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(ProductType.SUBS)
                        .build()
                }
            ).build()
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingProducts.ONE_TIME_IDS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(ProductType.INAPP)
                        .build()
                }
            ).build()

        val details = listOf(subsParams, inAppParams)
            .mapNotNull { params -> runCatching { billingClient.queryProductDetails(params) }.getOrNull() }
            .flatMap { it.productDetailsList ?: emptyList() }

        if (details.isNotEmpty()) {
            _productDetails.value = details.associateBy { it.productId }
        }
    }

    /** Refreshes owned entitlements from Play. Subscriptions that lapsed or were cancelled
     * naturally drop out of the returned list, which is how expiry is detected client-side. */
    suspend fun queryExistingPurchases() {
        if (_connectionState.value != BillingConnectionState.CONNECTED) return
        val subs = runCatching {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
            )
        }.getOrNull()
        val inApp = runCatching {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
            )
        }.getOrNull()

        // Only trust this snapshot (and only flip hasSyncedOnce) if Play actually answered at
        // least one of the two queries — otherwise a transient network error would look like
        // "the user owns nothing" and incorrectly revoke a cached entitlement.
        if (subs == null && inApp == null) return

        val activePurchases = subs?.purchasesList.orEmpty() + inApp?.purchasesList.orEmpty()
        val owned = mutableSetOf<String>()
        for (purchase in activePurchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!PurchaseVerifier.verifyPurchase(purchase.originalJson, purchase.signature)) continue
            acknowledgeIfNeeded(purchase)
            owned += purchase.products
        }
        _ownedProductIds.value = owned - BillingProducts.SMALL_TIP
        _hasSyncedOnce.value = true
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = _productDetails.value[productId]
        if (details == null) {
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Error("This item isn't available right now. Please try again later."))
            }
            return
        }
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        if (productId in BillingProducts.SUBSCRIPTION_IDS) {
            details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { paramsBuilder.setOfferToken(it) }
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                val list = purchases
                if (list.isNullOrEmpty()) {
                    scope.launch { _purchaseEvents.emit(PurchaseResult.Error("No purchase was returned.")) }
                } else {
                    list.forEach { handlePurchase(it) }
                }
            }
            BillingResponseCode.USER_CANCELED ->
                scope.launch { _purchaseEvents.emit(PurchaseResult.UserCancelled) }
            BillingResponseCode.ITEM_ALREADY_OWNED ->
                scope.launch {
                    _purchaseEvents.emit(PurchaseResult.AlreadyOwned)
                    queryExistingPurchases()
                }
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.NETWORK_ERROR ->
                scope.launch { _purchaseEvents.emit(PurchaseResult.NetworkError) }
            else ->
                scope.launch {
                    _purchaseEvents.emit(
                        PurchaseResult.Error(billingResult.debugMessage.ifBlank { "Purchase failed." })
                    )
                }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        scope.launch {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PENDING -> _purchaseEvents.emit(PurchaseResult.Pending)
                Purchase.PurchaseState.PURCHASED -> {
                    if (!PurchaseVerifier.verifyPurchase(purchase.originalJson, purchase.signature)) {
                        _purchaseEvents.emit(PurchaseResult.Error("Purchase could not be verified."))
                        return@launch
                    }
                    if (BillingProducts.SMALL_TIP in purchase.products) {
                        runCatching {
                            billingClient.consumePurchase(
                                ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                            )
                        }
                        _purchaseEvents.emit(PurchaseResult.TipReceived(BillingProducts.SMALL_TIP))
                    } else {
                        acknowledgeIfNeeded(purchase)
                        _ownedProductIds.update { it + purchase.products }
                        _purchaseEvents.emit(PurchaseResult.Success(purchase.products.first()))
                    }
                }
                else -> Unit
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            runCatching {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                )
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
