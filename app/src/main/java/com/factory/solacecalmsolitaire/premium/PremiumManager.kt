package com.factory.solacecalmsolitaire.premium

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.factory.solacecalmsolitaire.billing.BillingManager
import com.factory.solacecalmsolitaire.billing.BillingProducts
import com.factory.solacecalmsolitaire.billing.PremiumTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.premiumDataStore by preferencesDataStore(name = "solace_premium")

/**
 * Tracks whether the user is premium and which tier they hold. [BillingManager.ownedProductIds]
 * is the live source of truth; DataStore only caches the last known state so the UI doesn't
 * flash "free" while billing reconnects, and so entitlement survives process death.
 */
class PremiumManager(
    private val context: Context,
    private val billingManager: BillingManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val ACTIVE_TIER = stringPreferencesKey("active_tier")
        val HAS_SEEN_INITIAL_PAYWALL = booleanPreferencesKey("has_seen_initial_paywall")
    }

    private val cachedIsPremium: Flow<Boolean> =
        context.premiumDataStore.data.map { it[Keys.IS_PREMIUM] ?: false }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /** True once the cached entitlement has been read at least once, so callers that need to
     * make a one-shot decision (e.g. "show the first-launch paywall?") don't act on the
     * StateFlow's transient `false` default before the real cached value has loaded. */
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val activeTier: StateFlow<PremiumTier?> = billingManager.ownedProductIds
        .map { owned -> PremiumTier.entries.firstOrNull { it.productId in owned } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    val hasSeenInitialPaywall: Flow<Boolean> =
        context.premiumDataStore.data.map { it[Keys.HAS_SEEN_INITIAL_PAYWALL] ?: false }

    init {
        scope.launch {
            // Offline-first: trust the cache immediately so the UI doesn't flash "free" while
            // billing reconnects. Both steps run in this one coroutine, in order, so the cache
            // read can never race with (and clobber) the live sync result below.
            _isPremium.value = cachedIsPremium.first()
            _isLoaded.value = true

            // Once a real sync with Play completes, that snapshot is authoritative — including
            // revoking premium if a subscription lapsed while the app was closed. `cachedIsPremium`
            // is deliberately NOT an input here: persist() below writes back to it, and combining
            // on it would re-trigger this block on every write, looping forever.
            combine(billingManager.ownedProductIds, billingManager.hasSyncedOnce) { owned, synced ->
                synced to owned
            }.collect { (synced, owned) ->
                if (synced) {
                    val premium = owned.isNotEmpty()
                    _isPremium.value = premium
                    persist(premium, owned.firstOrNull { it != BillingProducts.SMALL_TIP })
                }
            }
        }
    }

    private suspend fun persist(isPremium: Boolean, tierProductId: String?) {
        context.premiumDataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
            if (tierProductId != null) {
                prefs[Keys.ACTIVE_TIER] = tierProductId
            } else {
                prefs.remove(Keys.ACTIVE_TIER)
            }
        }
    }

    suspend fun markInitialPaywallSeen() {
        context.premiumDataStore.edit { it[Keys.HAS_SEEN_INITIAL_PAYWALL] = true }
    }
}
