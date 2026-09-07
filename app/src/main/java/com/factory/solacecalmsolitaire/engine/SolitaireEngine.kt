package com.factory.solacecalmsolitaire.engine

import com.factory.solacecalmsolitaire.model.Card
import com.factory.solacecalmsolitaire.model.Rank
import com.factory.solacecalmsolitaire.model.freshDeck

/**
 * Pure, side-effect-free rules engine for Klondike Solitaire.
 * Every function returns a new [GameState]; invalid moves return null.
 */
object SolitaireEngine {

    private const val TABLEAU_COUNT = 7
    private const val FOUNDATION_COUNT = 4
    private const val SCORE_TO_FOUNDATION = 10
    private const val SCORE_WASTE_TO_TABLEAU = 5
    private const val SCORE_TABLEAU_FLIP = 5
    private const val SCORE_FOUNDATION_TO_TABLEAU = -10
    private const val SCORE_STOCK_RECYCLE = -2

    fun newGame(drawCount: Int): GameState {
        val deck = freshDeck().shuffled()
        val tableau = MutableList(TABLEAU_COUNT) { mutableListOf<Card>() }
        var cursor = 0
        for (col in 0 until TABLEAU_COUNT) {
            for (row in 0..col) {
                val faceUp = row == col
                tableau[col].add(deck[cursor].flipped(faceUp))
                cursor++
            }
        }
        val stock = deck.subList(cursor, deck.size).map { it.flipped(false) }
        return GameState(
            stock = stock,
            waste = emptyList(),
            foundations = List(FOUNDATION_COUNT) { emptyList() },
            tableau = tableau.map { it.toList() },
            drawCount = drawCount,
            score = 0,
            moves = 0,
            elapsedSeconds = 0,
            stockPasses = 0
        )
    }

    fun canDrawFromStock(state: GameState): Boolean =
        state.stock.isNotEmpty() || state.waste.isNotEmpty()

    fun drawFromStock(state: GameState): GameState {
        if (state.stock.isNotEmpty()) {
            val take = minOf(state.drawCount, state.stock.size)
            val drawn = state.stock.takeLast(take).reversed().map { it.flipped(true) }
            val remainingStock = state.stock.dropLast(take)
            return state.copy(
                stock = remainingStock,
                waste = state.waste + drawn,
                moves = state.moves + 1
            )
        }
        if (state.waste.isEmpty()) return state
        // Recycle waste back into stock, face down, preserving re-draw order.
        val recycled = state.waste.reversed().map { it.flipped(false) }
        return state.copy(
            stock = recycled,
            waste = emptyList(),
            moves = state.moves + 1,
            score = maxOf(0, state.score + SCORE_STOCK_RECYCLE),
            stockPasses = state.stockPasses + 1
        )
    }

    fun canPlaceOnTableau(card: Card, destination: List<Card>): Boolean {
        val top = destination.lastOrNull() ?: return card.rank == Rank.KING
        return top.isFaceUp &&
            top.rank.value == card.rank.value + 1 &&
            top.isOppositeColorOf(card)
    }

    fun canPlaceOnFoundation(card: Card, destination: List<Card>): Boolean {
        val top = destination.lastOrNull() ?: return card.rank == Rank.ACE
        return top.suit == card.suit && top.rank.value == card.rank.value - 1
    }

    fun moveWasteToFoundation(state: GameState): GameState? {
        val card = state.waste.lastOrNull() ?: return null
        val foundationIndex = foundationIndexFor(state, card) ?: return null
        val newFoundations = state.foundations.toMutableList()
        newFoundations[foundationIndex] = newFoundations[foundationIndex] + card
        return state.copy(
            waste = state.waste.dropLast(1),
            foundations = newFoundations,
            score = state.score + SCORE_TO_FOUNDATION,
            moves = state.moves + 1
        )
    }

    fun moveWasteToTableau(state: GameState, tableauIndex: Int): GameState? {
        val card = state.waste.lastOrNull() ?: return null
        val destination = state.tableau.getOrNull(tableauIndex) ?: return null
        if (!canPlaceOnTableau(card, destination)) return null
        val newTableau = state.tableau.toMutableList()
        newTableau[tableauIndex] = destination + card
        return state.copy(
            waste = state.waste.dropLast(1),
            tableau = newTableau,
            score = state.score + SCORE_WASTE_TO_TABLEAU,
            moves = state.moves + 1
        )
    }

    fun moveTableauToFoundation(state: GameState, tableauIndex: Int): GameState? {
        val pile = state.tableau.getOrNull(tableauIndex) ?: return null
        val card = pile.lastOrNull()?.takeIf { it.isFaceUp } ?: return null
        val foundationIndex = foundationIndexFor(state, card) ?: return null
        val newFoundations = state.foundations.toMutableList()
        newFoundations[foundationIndex] = newFoundations[foundationIndex] + card
        val newTableau = state.tableau.toMutableList()
        newTableau[tableauIndex] = revealTop(pile.dropLast(1))
        val flipBonus = if (pile.size >= 2 && !pile[pile.size - 2].isFaceUp) SCORE_TABLEAU_FLIP else 0
        return state.copy(
            tableau = newTableau,
            foundations = newFoundations,
            score = state.score + SCORE_TO_FOUNDATION + flipBonus,
            moves = state.moves + 1
        )
    }

    fun moveFoundationToTableau(state: GameState, foundationIndex: Int, tableauIndex: Int): GameState? {
        val foundation = state.foundations.getOrNull(foundationIndex) ?: return null
        val card = foundation.lastOrNull() ?: return null
        val destination = state.tableau.getOrNull(tableauIndex) ?: return null
        if (!canPlaceOnTableau(card, destination)) return null
        val newFoundations = state.foundations.toMutableList()
        newFoundations[foundationIndex] = foundation.dropLast(1)
        val newTableau = state.tableau.toMutableList()
        newTableau[tableauIndex] = destination + card
        return state.copy(
            foundations = newFoundations,
            tableau = newTableau,
            score = maxOf(0, state.score + SCORE_FOUNDATION_TO_TABLEAU),
            moves = state.moves + 1
        )
    }

    /**
     * Moves the face-up run starting at [cardIndex] within [fromIndex] tableau pile
     * onto [toIndex] tableau pile, provided the run forms a valid descending
     * alternating-color sequence and the destination accepts its lowest card.
     */
    fun moveTableauToTableau(state: GameState, fromIndex: Int, cardIndex: Int, toIndex: Int): GameState? {
        if (fromIndex == toIndex) return null
        val source = state.tableau.getOrNull(fromIndex) ?: return null
        val destination = state.tableau.getOrNull(toIndex) ?: return null
        if (cardIndex !in source.indices) return null
        val run = source.subList(cardIndex, source.size)
        if (run.any { !it.isFaceUp } || !isValidRun(run)) return null
        val movingCard = run.first()
        if (!canPlaceOnTableau(movingCard, destination)) return null

        val newTableau = state.tableau.toMutableList()
        newTableau[fromIndex] = revealTop(source.subList(0, cardIndex))
        newTableau[toIndex] = destination + run
        val flipBonus = if (cardIndex > 0 && !source[cardIndex - 1].isFaceUp) SCORE_TABLEAU_FLIP else 0
        return state.copy(
            tableau = newTableau,
            score = state.score + flipBonus,
            moves = state.moves + 1
        )
    }

    fun isGameWon(state: GameState): Boolean = state.isWon

    fun canAutoComplete(state: GameState): Boolean =
        !state.hasAnyFaceDownTableauCard && state.stock.isEmpty() && state.waste.isEmpty()

    /** Performs a single legal card-to-foundation move for auto-complete; null when none remain. */
    fun autoCompleteOneStep(state: GameState): GameState? {
        state.waste.lastOrNull()?.let { card ->
            foundationIndexFor(state, card)?.let { return moveWasteToFoundation(state) }
        }
        for (index in state.tableau.indices) {
            val card = state.tableau[index].lastOrNull()?.takeIf { it.isFaceUp } ?: continue
            if (foundationIndexFor(state, card) != null) {
                return moveTableauToFoundation(state, index)
            }
        }
        return null
    }

    private fun isValidRun(run: List<Card>): Boolean {
        for (i in 0 until run.size - 1) {
            val current = run[i]
            val next = run[i + 1]
            if (current.rank.value != next.rank.value + 1 || !current.isOppositeColorOf(next)) {
                return false
            }
        }
        return true
    }

    private fun revealTop(pile: List<Card>): List<Card> {
        if (pile.isEmpty()) return pile
        val top = pile.last()
        return if (top.isFaceUp) pile else pile.dropLast(1) + top.flipped(true)
    }

    private fun foundationIndexFor(state: GameState, card: Card): Int? {
        val index = state.foundations.indexOfFirst { it.isNotEmpty() && it.last().suit == card.suit }
        if (index != -1) {
            return if (canPlaceOnFoundation(card, state.foundations[index])) index else null
        }
        if (card.rank != Rank.ACE) return null
        val emptyIndex = state.foundations.indexOfFirst { it.isEmpty() }
        return if (emptyIndex != -1) emptyIndex else null
    }
}
