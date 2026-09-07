package com.factory.solacecalmsolitaire.model

data class Card(
    val id: Int,
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = false
) {
    val isRed: Boolean get() = suit.isRed

    fun isOppositeColorOf(other: Card): Boolean = isRed != other.isRed

    fun flipped(faceUp: Boolean): Card = if (isFaceUp == faceUp) this else copy(isFaceUp = faceUp)

    /** Human-readable description for accessibility services (e.g. "Ace of Spades, face up"). */
    val accessibleDescription: String
        get() = if (isFaceUp) "${rank.fullName} of ${suit.fullName}, face up" else "Face-down card"
}

fun freshDeck(): List<Card> {
    var id = 0
    return buildList {
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                add(Card(id = id++, suit = suit, rank = rank, isFaceUp = false))
            }
        }
    }
}
