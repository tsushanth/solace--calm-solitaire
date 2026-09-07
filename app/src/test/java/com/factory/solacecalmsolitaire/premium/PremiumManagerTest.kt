package com.factory.solacecalmsolitaire.premium

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.factory.solacecalmsolitaire.billing.BillingManager
import com.factory.solacecalmsolitaire.billing.BillingProducts
import com.factory.solacecalmsolitaire.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Application

@RunWith(AndroidJUnit4::class)
class PremiumManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun fakeBillingManager(
        owned: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet()),
        synced: MutableStateFlow<Boolean> = MutableStateFlow(false)
    ): BillingManager {
        val manager = mockk<BillingManager>(relaxed = true)
        every { manager.ownedProductIds } returns owned
        every { manager.hasSyncedOnce } returns synced
        return manager
    }

    @Test
    fun `initial state is not premium until cache loads`() = runTest {
        val premiumManager = PremiumManager(context, fakeBillingManager())

        premiumManager.isLoaded.test {
            assertFalse(awaitItem())
            assertTrue(awaitItem())
        }
        assertFalse(premiumManager.isPremium.value)
    }

    @Test
    fun `syncing owned products flips isPremium to true`() = runTest {
        val owned = MutableStateFlow<Set<String>>(emptySet())
        val synced = MutableStateFlow(false)
        val premiumManager = PremiumManager(context, fakeBillingManager(owned, synced))

        premiumManager.isPremium.test {
            assertFalse(awaitItem())
            owned.value = setOf(BillingProducts.YEARLY)
            synced.value = true
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `synced with no owned products keeps isPremium false`() = runTest {
        val owned = MutableStateFlow<Set<String>>(emptySet())
        val synced = MutableStateFlow(false)
        val premiumManager = PremiumManager(context, fakeBillingManager(owned, synced))

        premiumManager.isLoaded.test { awaitItem(); awaitItem() }
        synced.value = true

        assertFalse(premiumManager.isPremium.value)
    }

    @Test
    fun `activeTier reflects owned product matching a premium tier`() = runTest {
        val owned = MutableStateFlow<Set<String>>(emptySet())
        val synced = MutableStateFlow(false)
        val premiumManager = PremiumManager(context, fakeBillingManager(owned, synced))

        premiumManager.activeTier.test {
            assertEquals(null, awaitItem())
            owned.value = setOf(BillingProducts.LIFETIME)
            assertEquals(com.factory.solacecalmsolitaire.billing.PremiumTier.LIFETIME, awaitItem())
        }
    }

    @Test
    fun `premium status persists across manager instances`() = runTest {
        val owned = MutableStateFlow<Set<String>>(emptySet())
        val synced = MutableStateFlow(false)
        val first = PremiumManager(context, fakeBillingManager(owned, synced))

        first.isPremium.test {
            assertFalse(awaitItem())
            owned.value = setOf(BillingProducts.MONTHLY)
            synced.value = true
            assertTrue(awaitItem())
        }

        val second = PremiumManager(context, fakeBillingManager())
        second.isLoaded.test {
            assertFalse(awaitItem())
            assertTrue(awaitItem())
        }
        assertTrue(second.isPremium.value)
    }

    @Test
    fun `markInitialPaywallSeen persists flag`() = runTest {
        val premiumManager = PremiumManager(context, fakeBillingManager())

        premiumManager.hasSeenInitialPaywall.test {
            assertFalse(awaitItem())
            premiumManager.markInitialPaywallSeen()
            assertTrue(awaitItem())
        }
    }
}
