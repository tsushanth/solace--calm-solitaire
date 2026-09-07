package com.factory.solacecalmsolitaire

import android.app.Application
import com.factory.solacecalmsolitaire.billing.BillingManager
import com.factory.solacecalmsolitaire.data.datastore.SettingsDataStore
import com.factory.solacecalmsolitaire.data.local.AppDatabase
import com.factory.solacecalmsolitaire.data.repository.StatsRepository
import com.factory.solacecalmsolitaire.premium.PremiumManager

open class SolaceApplication : Application() {

    open val statsRepository: StatsRepository by lazy {
        StatsRepository(AppDatabase.getInstance(this).gameResultDao())
    }

    open val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(this)
    }

    open val billingManager: BillingManager by lazy {
        BillingManager(this)
    }

    open val premiumManager: PremiumManager by lazy {
        PremiumManager(this, billingManager)
    }

    override fun onCreate() {
        super.onCreate()
        billingManager.startConnection()
    }
}
