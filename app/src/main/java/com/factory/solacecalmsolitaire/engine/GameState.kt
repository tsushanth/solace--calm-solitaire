package com.factory.solacecalmsolitaire.engine

import com.factory.solacecalmsolitaire.model.Card

data class GameState(
    val stock: List<Card> = emptyList(),
    val waste: List<Card> = emptyList(),
    val foundations: List<List<Card>> = List(4) { emptyList() },
    val tableau: List<List<Card>> = List(7) { emptyList() },
    val drawCount: Int = 3,
    val score: Int = 0,
    val moves: Int = 0,
    val elapsedSeconds: Int = 0,
    val stockPasses: Int = 0
) {
    val isWon: Boolean
        get() = foundations.all { it.size == 13 }

    val hasAnyFaceDownTableauCard: Boolean
        get() = tableau.any { pile -> pile.any { !it.isFaceUp } }
}
