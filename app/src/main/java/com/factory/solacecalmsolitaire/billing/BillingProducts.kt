package com.factory.solacecalmsolitaire.billing

import com.android.billingclient.api.BillingClient.ProductType

/** Central registry of Google Play product IDs. Configure matching entries in Play Console. */
object BillingProducts {
    private const val SUBSCRIPTION_BASE = "com.factory.solacecalmsolitaire.subscription"

    const val WEEKLY = "$SUBSCRIPTION_BASE.weekly"
    const val MONTHLY = "$SUBSCRIPTION_BASE.monthly"
    const val YEARLY = "$SUBSCRIPTION_BASE.yearly"
    const val LIFETIME = "com.factory.solacecalmsolitaire.lifetime"
    const val SMALL_TIP = "com.factory.solacecalmsolitaire.small_iap"

    val SUBSCRIPTION_IDS = setOf(WEEKLY, MONTHLY, YEARLY)
    val ONE_TIME_IDS = setOf(LIFETIME, SMALL_TIP)

    /** Product IDs that, once owned, grant premium access (excludes the one-time tip). */
    val PREMIUM_UNLOCK_IDS = SUBSCRIPTION_IDS + LIFETIME

    fun productType(productId: String): String =
        if (productId in SUBSCRIPTION_IDS) ProductType.SUBS else ProductType.INAPP
}

enum class PremiumTier(
    val productId: String,
    val isSubscription: Boolean,
    val fallbackPrice: String,
    val fallbackPeriod: String
) {
    WEEKLY(BillingProducts.WEEKLY, true, "$3.19", "/week"),
    MONTHLY(BillingProducts.MONTHLY, true, "$8.00", "/month"),
    YEARLY(BillingProducts.YEARLY, true, "$23.99", "/year"),
    LIFETIME(BillingProducts.LIFETIME, false, "$47.98", "one-time")
}

/** The small one-time IAP is a consumable "tip" — it supports development but does not unlock premium. */
object SmallTip {
    const val PRODUCT_ID = BillingProducts.SMALL_TIP
    const val FALLBACK_PRICE = "$0.99"
}
