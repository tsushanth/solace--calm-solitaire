package com.factory.solacecalmsolitaire.ui.theme

import androidx.compose.ui.graphics.Color
import com.factory.solacecalmsolitaire.premium.PremiumFeature

enum class FeltTheme(val storageKey: String, val label: String, val color: Color, val feature: PremiumFeature) {
    CLASSIC("classic", "Classic", DarkBackground, PremiumFeature.CLASSIC_FELT_THEME),
    MIDNIGHT("midnight", "Midnight", MidnightFeltBackground, PremiumFeature.MIDNIGHT_FELT_THEME),
    CRIMSON("crimson", "Crimson", CrimsonFeltBackground, PremiumFeature.CRIMSON_FELT_THEME);

    val isPremium: Boolean get() = feature.isPremium

    companion object {
        fun fromStorageKey(key: String): FeltTheme = entries.firstOrNull { it.storageKey == key } ?: CLASSIC
    }
}
