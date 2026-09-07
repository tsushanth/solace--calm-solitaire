package com.factory.solacecalmsolitaire.data.repository

data class StatsSummary(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val winPercentage: Int = 0,
    val bestTimeSeconds: Int? = null,
    val bestScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)
