package com.factory.solacecalmsolitaire.billing

import android.app.Activity
import android.content.Context
import app.cash.turbine.test
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.factory.solacecalmsolitaire.util.MainDispatcherRule
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BillingManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val billingClient = mockk<BillingClient>(relaxed = true)
    private lateinit var billingManager: BillingManager

    @Before
    fun setUp() {
        // queryPurchasesAsync has a deprecated String-typed overload alongside the
        // QueryPurchasesParams one, which makes a bare `BillingClient::queryPurchasesAsync`
        // callable reference ambiguous. Mocking the whole extension-function class by name
        // sidesteps that and covers all four extension functions used below.
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")

        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { billingClient.connectionState } returns BillingClient.ConnectionState.DISCONNECTED

        billingManager = BillingManager(context) { _, _ -> billingClient }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.android.billingclient.api.BillingClientKotlinKt")
    }

    private fun okResult(code: Int = BillingResponseCode.OK, message: String = "") =
        BillingResult.newBuilder().setResponseCode(code).setDebugMessage(message).build()

    private fun purchaseJson(
        productId: String,
        state: Int = Purchase.PurchaseState.PURCHASED,
        token: String = "token-$productId",
        acknowledged: Boolean = true
    ) = """
        {
          "orderId": "order-$productId",
          "packageName": "com.factory.solacecalmsolitaire",
          "productIds": ["$productId"],
          "purchaseTime": 1000,
          "purchaseState": $state,
          "purchaseToken": "$token",
          "quantity": 1,
          "autoRenewing": true,
          "acknowledged": $acknowledged
        }
    """.trimIndent()

    private fun purchase(
        productId: String,
        state: Int = Purchase.PurchaseState.PURCHASED,
        token: String = "token-$productId",
        acknowledged: Boolean = true
    ) = Purchase(purchaseJson(productId, state, token, acknowledged), "signature-$productId")

    private fun captureConnectionListener(): CapturingSlot<BillingClientStateListener> {
        val slot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(slot)) } answers { }
        return slot
    }

    private fun connectSuccessfully() {
        val slot = captureConnectionListener()
        billingManager.startConnection()
        slot.captured.onBillingSetupFinished(okResult())
        every { billingClient.connectionState } returns BillingClient.ConnectionState.CONNECTED
    }

    // ---- Connection / product loading ----

    @Test
    fun `startConnection success loads products and purchases`() = runTest {
        val productDetails = mockk<ProductDetails>(relaxed = true)
        every { productDetails.productId } returns BillingProducts.YEARLY
        coEvery { billingClient.queryProductDetails(any()) } returns
            ProductDetailsResult(okResult(), listOf(productDetails))
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
            PurchasesResult(okResult(), listOf(purchase(BillingProducts.YEARLY)))

        connectSuccessfully()

        assertEquals(BillingConnectionState.CONNECTED, billingManager.connectionState.value)
        assertTrue(BillingProducts.YEARLY in billingManager.productDetails.value)
        assertTrue(BillingProducts.YEARLY in billingManager.ownedProductIds.value)
        assertTrue(billingManager.hasSyncedOnce.value)
    }

    @Test
    fun `startConnection billing unavailable sets state`() = runTest {
        val slot = captureConnectionListener()
        billingManager.startConnection()
        slot.captured.onBillingSetupFinished(okResult(BillingResponseCode.BILLING_UNAVAILABLE))

        assertEquals(BillingConnectionState.UNAVAILABLE, billingManager.connectionState.value)
    }

    @Test
    fun `startConnection does not reconnect when already connected`() {
        every { billingClient.connectionState } returns BillingClient.ConnectionState.CONNECTED
        billingManager.startConnection()
        verify(exactly = 0) { billingClient.startConnection(any()) }
    }

    @Test
    fun `queryProductDetails is a no-op when not connected`() = runTest {
        billingManager.queryProductDetails()
        coVerify(exactly = 0) { billingClient.queryProductDetails(any()) }
        assertTrue(billingManager.productDetails.value.isEmpty())
    }

    @Test
    fun `queryExistingPurchases acknowledges unacknowledged purchases`() = runTest {
        coEvery { billingClient.queryProductDetails(any()) } returns ProductDetailsResult(okResult(), emptyList())
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
            PurchasesResult(okResult(), listOf(purchase(BillingProducts.LIFETIME, acknowledged = false)))
        coEvery { billingClient.acknowledgePurchase(any()) } returns okResult()

        connectSuccessfully()

        coVerify { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>()) }
        assertTrue(BillingProducts.LIFETIME in billingManager.ownedProductIds.value)
    }

    @Test
    fun `queryExistingPurchases excludes small tip from owned products`() = runTest {
        coEvery { billingClient.queryProductDetails(any()) } returns ProductDetailsResult(okResult(), emptyList())
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns
            PurchasesResult(okResult(), listOf(purchase(BillingProducts.SMALL_TIP)))

        connectSuccessfully()

        assertFalse(BillingProducts.SMALL_TIP in billingManager.ownedProductIds.value)
    }

    @Test
    fun `queryExistingPurchases keeps hasSyncedOnce false when both queries fail`() = runTest {
        coEvery { billingClient.queryProductDetails(any()) } returns ProductDetailsResult(okResult(), emptyList())
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } throws RuntimeException("network down")

        connectSuccessfully()

        assertFalse(billingManager.hasSyncedOnce.value)
        assertTrue(billingManager.ownedProductIds.value.isEmpty())
    }

    // ---- Purchase flow ----

    @Test
    fun `launchPurchaseFlow emits error when product not loaded`() = runTest {
        val activity = mockk<Activity>(relaxed = true)
        billingManager.purchaseEvents.test {
            billingManager.launchPurchaseFlow(activity, BillingProducts.MONTHLY)
            val event = awaitItem()
            assertTrue(event is PurchaseResult.Error)
        }
        verify(exactly = 0) { billingClient.launchBillingFlow(any(), any()) }
    }

    @Test
    fun `launchPurchaseFlow launches billing flow when product is loaded`() = runTest {
        val productDetails = mockk<ProductDetails>(relaxed = true)
        every { productDetails.productId } returns BillingProducts.MONTHLY
        every { productDetails.subscriptionOfferDetails } returns null
        coEvery { billingClient.queryProductDetails(any()) } returns
            ProductDetailsResult(okResult(), listOf(productDetails))
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns PurchasesResult(okResult(), emptyList())
        connectSuccessfully()

        val activity = mockk<Activity>(relaxed = true)
        billingManager.launchPurchaseFlow(activity, BillingProducts.MONTHLY)

        verify { billingClient.launchBillingFlow(activity, any()) }
    }

    // ---- onPurchasesUpdated ----

    @Test
    fun `onPurchasesUpdated success grants entitlement and emits Success`() = runTest {
        coEvery { billingClient.acknowledgePurchase(any()) } returns okResult()

        billingManager.purchaseEvents.test {
            billingManager.onPurchasesUpdated(okResult(), mutableListOf(purchase(BillingProducts.YEARLY, acknowledged = false)))
            val event = awaitItem()
            assertTrue(event is PurchaseResult.Success)
            assertEquals(BillingProducts.YEARLY, (event as PurchaseResult.Success).productId)
        }
        assertTrue(BillingProducts.YEARLY in billingManager.ownedProductIds.value)
        coVerify { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>()) }
    }

    @Test
    fun `onPurchasesUpdated consumes and reports tip without granting premium`() = runTest {
        coEvery { billingClient.consumePurchase(any()) } returns ConsumeResult(okResult(), "token")

        billingManager.purchaseEvents.test {
            billingManager.onPurchasesUpdated(okResult(), mutableListOf(purchase(BillingProducts.SMALL_TIP)))
            val event = awaitItem()
            assertTrue(event is PurchaseResult.TipReceived)
        }
        assertFalse(BillingProducts.SMALL_TIP in billingManager.ownedProductIds.value)
        coVerify { billingClient.consumePurchase(any<ConsumeParams>()) }
    }

    @Test
    fun `onPurchasesUpdated with OK and empty list emits Error`() = runTest {
        billingManager.purchaseEvents.test {
            billingManager.onPurchasesUpdated(okResult(), mutableListOf())
            assertTrue(awaitItem() is PurchaseResult.Error)
        }
    }

    @Test
    fun `onPurchasesUpdated user cancelled emits UserCancelled`() = runTest {
        billingManager.purchaseEvents.test {
            billingManager.onPurchasesUpdated(okResult(BillingResponseCode.USER_CANCELED), null)
            assertEquals(PurchaseResult.UserCancelled, awaitItem())
        }
    }

    @Test
    fun `onPurchasesUpdated already owned emits AlreadyOwned and re-syncs`() = runTest {
        coEvery { billingClient.queryProductDetails(any()) } returns ProductDetailsResult(okResult(), emptyList())
        coEvery { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) } returns PurchasesResult(okResult(), emptyList())

        billingManager.purchaseEvents.test {
            billingManager.onPurchasesUpdated(okResult(BillingResponseCode.ITEM_ALREADY_OWNED), null)
            assertEquals(PurchaseResult.AlreadyOwned, awaitItem())
        }
        coVerify { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>()) }
    }

    @Test
    fun `onPurchasesUpdated network error emits NetworkError`() = runTest {
        billingManager.purchaseEvents.test {
            billingManager.onPurchasesUpdated(okResult(BillingResponseCode.NETWORK_ERROR), null)
            assertEquals(PurchaseResult.NetworkError, awaitItem())
        }
    }

    @Test
    fun `onPurchasesUpdated other failure emits Error with debug message`() = runTest {
        billingManager.purchaseEvents.test {
            billingManager.onPurchasesUpdated(okResult(BillingResponseCode.DEVELOPER_ERROR, "bad request"), null)
            val event = awaitItem()
            assertTrue(event is PurchaseResult.Error)
            assertEquals("bad request", (event as PurchaseResult.Error).message)
        }
    }

    @Test
    fun `endConnection delegates to billing client`() {
        billingManager.endConnection()
        verify { billingClient.endConnection() }
    }
}
