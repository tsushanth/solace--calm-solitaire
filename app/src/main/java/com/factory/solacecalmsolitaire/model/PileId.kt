package com.factory.solacecalmsolitaire.model

sealed class PileId {
    data object Stock : PileId()
    data object Waste : PileId()
    data class Foundation(val index: Int) : PileId()
    data class Tableau(val index: Int) : PileId()
}
