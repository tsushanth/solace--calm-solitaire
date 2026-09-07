package com.factory.solacecalmsolitaire

import com.factory.solacecalmsolitaire.billing.BillingManager
import com.factory.solacecalmsolitaire.data.datastore.SettingsDataStore
import com.factory.solacecalmsolitaire.data.repository.StatsRepository
import com.factory.solacecalmsolitaire.premium.PremiumManager

/**
 * Test double for [SolaceApplication] used under Robolectric. Production dependencies are
 * lazily created for real (real DataStore, real in-memory-backed repository) unless a test
 * assigns an override, which lets each test control only the collaborators it cares about
 * without standing up a real Play Billing connection.
 */
class TestSolaceApplication : SolaceApplication() {

    var billingManagerOverride: BillingManager? = null
    var premiumManagerOverride: PremiumManager? = null
    var statsRepositoryOverride: StatsRepository? = null
    var settingsDataStoreOverride: SettingsDataStore? = null

    override val billingManager: BillingManager
        get() = billingManagerOverride ?: super.billingManager

    override val premiumManager: PremiumManager
        get() = premiumManagerOverride ?: super.premiumManager

    override val statsRepository: StatsRepository
        get() = statsRepositoryOverride ?: super.statsRepository

    override val settingsDataStore: SettingsDataStore
        get() = settingsDataStoreOverride ?: super.settingsDataStore

    /** Skips [SolaceApplication.onCreate]'s real Play Billing connection during test app creation. */
    override fun onCreate() = Unit
}
