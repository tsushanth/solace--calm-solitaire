package com.factory.solacecalmsolitaire.viewmodel

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.factory.solacecalmsolitaire.TestSolaceApplication
import com.factory.solacecalmsolitaire.billing.BillingConnectionState
import com.factory.solacecalmsolitaire.billing.BillingManager
import com.factory.solacecalmsolitaire.billing.BillingProducts
import com.factory.solacecalmsolitaire.billing.PremiumTier
import com.factory.solacecalmsolitaire.billing.PurchaseResult
import com.factory.solacecalmsolitaire.premium.PremiumManager
import com.factory.solacecalmsolitaire.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = TestSolaceApplication::class)
class PaywallViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: TestSolaceApplication
    private lateinit var connectionState: MutableStateFlow<BillingConnectionState>
    private lateinit var productDetails: MutableStateFlow<Map<String, com.android.billingclient.api.ProductDetails>>
    private lateinit var purchaseEvents: MutableSharedFlow<PurchaseResult>
    private lateinit var isPremium: MutableStateFlow<Boolean>
    private lateinit var activeTier: MutableStateFlow<PremiumTier?>
    private lateinit var billingManager: BillingManager
    private lateinit var premiumManager: PremiumManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()

        connectionState = MutableStateFlow(BillingConnectionState.CONNECTING)
        productDetails = MutableStateFlow(emptyMap())
        purchaseEvents = MutableSharedFlow(extraBufferCapacity = 4)
        billingManager = mockk(relaxed = true)
        every { billingManager.connectionState } returns connectionState
        every { billingManager.productDetails } returns productDetails
        every { billingManager.purchaseEvents } returns purchaseEvents
        app.billingManagerOverride = billingManager

        isPremium = MutableStateFlow(false)
        activeTier = MutableStateFlow(null)
        premiumManager = mockk(relaxed = true)
        every { premiumManager.isPremium } returns isPremium
        every { premiumManager.activeTier } returns activeTier
        app.premiumManagerOverride = premiumManager
    }

    @Test
    fun `init starts the billing connection`() {
        PaywallViewModel(app)
        verify { billingManager.startConnection() }
    }

    @Test
    fun `uiState reflects billing and premium snapshot`() = runTest {
        val viewModel = PaywallViewModel(app)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(BillingConnectionState.CONNECTING, initial.connectionState)
            assertFalse(initial.isPremium)

            connectionState.value = BillingConnectionState.CONNECTED
            assertEquals(BillingConnectionState.CONNECTED, awaitItem().connectionState)

            isPremium.value = true
            activeTier.value = PremiumTier.YEARLY
            val premiumState = awaitItem()
            assertTrue(premiumState.isPremium)
            assertEquals(PremiumTier.YEARLY, premiumState.activeTier)
        }
    }

    @Test
    fun `purchase launches billing flow and sets isPurchasing`() = runTest {
        val viewModel = PaywallViewModel(app)
        val activity = mockk<Activity>(relaxed = true)

        viewModel.uiState.test {
            awaitItem() // settled initial state

            viewModel.purchase(activity, PremiumTier.MONTHLY)
            assertTrue(awaitItem().isPurchasing)
        }
        verify { billingManager.launchPurchaseFlow(activity, BillingProducts.MONTHLY) }
    }

    @Test
    fun `successful purchase event clears isPurchasing and sets a message`() = runTest {
        val viewModel = PaywallViewModel(app)
        val activity = mockk<Activity>(relaxed = true)

        viewModel.uiState.test {
            awaitItem() // settled initial state
            viewModel.purchase(activity, PremiumTier.MONTHLY)
            assertTrue(awaitItem().isPurchasing)

            viewModel.message.test {
                assertNull(awaitItem())
                purchaseEvents.emit(PurchaseResult.Success(BillingProducts.MONTHLY))
                assertEquals("You're premium! Enjoy the full game.", awaitItem())
            }
            assertFalse(awaitItem().isPurchasing)
        }
    }

    @Test
    fun `user cancelled purchase clears isPurchasing without a message`() = runTest {
        val viewModel = PaywallViewModel(app)
        val activity = mockk<Activity>(relaxed = true)

        viewModel.uiState.test {
            awaitItem()
            viewModel.purchase(activity, PremiumTier.MONTHLY)
            assertTrue(awaitItem().isPurchasing)

            purchaseEvents.emit(PurchaseResult.UserCancelled)

            assertFalse(awaitItem().isPurchasing)
        }
        assertNull(viewModel.message.value)
    }

    @Test
    fun `restorePurchases queries billing and reports found purchases`() = runTest {
        val viewModel = PaywallViewModel(app)
        isPremium.value = true

        viewModel.uiState.test {
            awaitItem() // settled initial state (isPremium already true at construction)

            viewModel.message.test {
                assertNull(awaitItem())
                viewModel.restorePurchases()
                assertEquals("Purchases restored.", awaitItem())
            }
        }
        coVerify { billingManager.queryExistingPurchases() }
    }

    @Test
    fun `restorePurchases reports nothing found when still free`() = runTest {
        val viewModel = PaywallViewModel(app)

        viewModel.message.test {
            assertNull(awaitItem())
            viewModel.restorePurchases()
            assertEquals("No previous purchases found for this account.", awaitItem())
        }
    }

    @Test
    fun `retryConnection restarts the billing connection`() {
        val viewModel = PaywallViewModel(app)
        viewModel.retryConnection()

        verify(exactly = 2) { billingManager.startConnection() } // once on init, once on retry
    }

    @Test
    fun `consumeMessage clears the current message`() = runTest {
        val viewModel = PaywallViewModel(app)

        viewModel.message.test {
            assertNull(awaitItem())
            purchaseEvents.emit(PurchaseResult.AlreadyOwned)
            assertEquals("You already own this.", awaitItem())

            viewModel.consumeMessage()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `markInitialPaywallSeen delegates to premium manager`() {
        val viewModel = PaywallViewModel(app)
        viewModel.markInitialPaywallSeen()

        coVerify { premiumManager.markInitialPaywallSeen() }
    }
}
