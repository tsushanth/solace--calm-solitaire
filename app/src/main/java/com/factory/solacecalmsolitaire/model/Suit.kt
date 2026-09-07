package com.factory.solacecalmsolitaire.model

enum class Suit(val symbol: String, val isRed: Boolean, val fullName: String) {
    SPADES("♠", isRed = false, fullName = "Spades"),
    HEARTS("♥", isRed = true, fullName = "Hearts"),
    DIAMONDS("♦", isRed = true, fullName = "Diamonds"),
    CLUBS("♣", isRed = false, fullName = "Clubs")
}
