package com.factory.solacecalmsolitaire.premium

/**
 * Registry of gateable app capabilities. `isPremium = true` entries require an active
 * subscription or the lifetime unlock; the rest remain free. About 60% of listed
 * capabilities are premium.
 */
enum class PremiumFeature(val isPremium: Boolean, val title: String, val description: String) {
    DRAW_THREE(
        isPremium = false,
        title = "Draw Three",
        description = "Classic three-card draw"
    ),
    BASIC_STATISTICS(
        isPremium = false,
        title = "Basic Statistics",
        description = "Games played, games won, and win rate"
    ),
    CLASSIC_FELT_THEME(
        isPremium = false,
        title = "Classic Felt",
        description = "The original green felt table"
    ),
    DRAW_ONE(
        isPremium = true,
        title = "Draw One Mode",
        description = "The purist's challenge — draw one card at a time"
    ),
    UNLIMITED_UNDO(
        isPremium = true,
        title = "Unlimited Undo",
        description = "Take back as many moves as you like, every game"
    ),
    AUTO_COMPLETE(
        isPremium = true,
        title = "Auto-Complete",
        description = "Finish winnable games instantly with one tap"
    ),
    ADVANCED_STATISTICS(
        isPremium = true,
        title = "Advanced Statistics",
        description = "Best time, best score, and win streaks"
    ),
    MIDNIGHT_FELT_THEME(
        isPremium = true,
        title = "Midnight Felt",
        description = "A deep blue table theme"
    ),
    CRIMSON_FELT_THEME(
        isPremium = true,
        title = "Crimson Felt",
        description = "A rich wine-red table theme"
    );

    companion object {
        val premiumHighlights: List<PremiumFeature> get() = entries.filter { it.isPremium }
    }
}
