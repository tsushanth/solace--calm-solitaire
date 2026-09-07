package com.factory.solacecalmsolitaire.model

enum class Rank(val value: Int, val label: String, val fullName: String) {
    ACE(1, "A", "Ace"),
    TWO(2, "2", "Two"),
    THREE(3, "3", "Three"),
    FOUR(4, "4", "Four"),
    FIVE(5, "5", "Five"),
    SIX(6, "6", "Six"),
    SEVEN(7, "7", "Seven"),
    EIGHT(8, "8", "Eight"),
    NINE(9, "9", "Nine"),
    TEN(10, "10", "Ten"),
    JACK(11, "J", "Jack"),
    QUEEN(12, "Q", "Queen"),
    KING(13, "K", "King")
}
