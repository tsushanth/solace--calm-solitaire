package com.factory.solacecalmsolitaire.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.solacecalmsolitaire.SolaceApplication
import com.factory.solacecalmsolitaire.billing.BillingConnectionState
import com.factory.solacecalmsolitaire.billing.BillingManager
import com.factory.solacecalmsolitaire.billing.BillingProducts
import com.factory.solacecalmsolitaire.billing.PremiumTier
import com.factory.solacecalmsolitaire.billing.PurchaseResult
import com.factory.solacecalmsolitaire.billing.SmallTip
import com.factory.solacecalmsolitaire.premium.PremiumManager
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaywallUiState(
    val connectionState: BillingConnectionState = BillingConnectionState.CONNECTING,
    val productDetails: Map<String, ProductDetails> = emptyMap(),
    val isPremium: Boolean = false,
    val activeTier: PremiumTier? = null,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false
) {
    fun priceLabel(tier: PremiumTier): String {
        val formatted = productDetails[tier.productId]?.let { details ->
            if (tier.isSubscription) {
                details.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    ?.formattedPrice
            } else {
                details.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
        return formatted ?: tier.fallbackPrice
    }

    val tipPriceLabel: String
        get() = productDetails[SmallTip.PRODUCT_ID]?.oneTimePurchaseOfferDetails?.formattedPrice
            ?: SmallTip.FALLBACK_PRICE
}

class PaywallViewModel(application: Application) : AndroidViewModel(application) {

    private val solaceApplication = application as SolaceApplication
    private val billingManager: BillingManager = solaceApplication.billingManager
    private val premiumManager: PremiumManager = solaceApplication.premiumManager

    private val _message = MutableStateFlow<String?>(null)
    private val _isPurchasing = MutableStateFlow(false)
    private val _isRestoring = MutableStateFlow(false)

    private data class BillingSnapshot(
        val connectionState: BillingConnectionState,
        val productDetails: Map<String, ProductDetails>,
        val isPremium: Boolean,
        val activeTier: PremiumTier?
    )

    private val billingSnapshot = combine(
        billingManager.connectionState,
        billingManager.productDetails,
        premiumManager.isPremium,
        premiumManager.activeTier
    ) { connection, details, isPremium, tier -> BillingSnapshot(connection, details, isPremium, tier) }

    val uiState: StateFlow<PaywallUiState> = combine(
        billingSnapshot, _isPurchasing, _isRestoring
    ) { snapshot, purchasing, restoring ->
        PaywallUiState(
            connectionState = snapshot.connectionState,
            productDetails = snapshot.productDetails,
            isPremium = snapshot.isPremium,
            activeTier = snapshot.activeTier,
            isPurchasing = purchasing,
            isRestoring = restoring
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaywallUiState())

    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        billingManager.startConnection()
        viewModelScope.launch {
            billingManager.purchaseEvents.collect { result ->
                _isPurchasing.value = false
                _message.value = when (result) {
                    is PurchaseResult.Success -> "You're premium! Enjoy the full game."
                    is PurchaseResult.TipReceived -> "Thank you for the support!"
                    PurchaseResult.Pending -> "Your purchase is pending — it'll unlock once it completes."
                    PurchaseResult.UserCancelled -> null
                    PurchaseResult.AlreadyOwned -> "You already own this."
                    PurchaseResult.NetworkError -> "No connection. Check your network and try again."
                    is PurchaseResult.Error -> result.message
                }
            }
        }
    }

    fun purchase(activity: Activity, tier: PremiumTier) {
        _isPurchasing.value = true
        billingManager.launchPurchaseFlow(activity, tier.productId)
    }

    fun purchaseTip(activity: Activity) {
        _isPurchasing.value = true
        billingManager.launchPurchaseFlow(activity, BillingProducts.SMALL_TIP)
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _isRestoring.value = true
            billingManager.queryExistingPurchases()
            _isRestoring.value = false
            _message.value = if (premiumManager.isPremium.value) {
                "Purchases restored."
            } else {
                "No previous purchases found for this account."
            }
        }
    }

    fun retryConnection() {
        billingManager.startConnection()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun markInitialPaywallSeen() {
        viewModelScope.launch { premiumManager.markInitialPaywallSeen() }
    }
}
