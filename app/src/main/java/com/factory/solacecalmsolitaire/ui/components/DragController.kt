package com.factory.solacecalmsolitaire.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.factory.solacecalmsolitaire.model.Card
import com.factory.solacecalmsolitaire.model.PileId

/** Tracks the card(s) currently being dragged and each pile's on-screen bounds for drop detection. */
class DragController {
    var source by mutableStateOf<PileId?>(null)
        private set
    var sourceIndex by mutableStateOf(0)
        private set
    var cards by mutableStateOf<List<Card>>(emptyList())
        private set
    var origin by mutableStateOf(Offset.Zero)
        private set
    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    val isDragging: Boolean get() = source != null

    val pileBounds: SnapshotStateMap<PileId, Rect> = SnapshotStateMap()

    fun beginDrag(source: PileId, sourceIndex: Int, cards: List<Card>, origin: Offset) {
        this.source = source
        this.sourceIndex = sourceIndex
        this.cards = cards
        this.origin = origin
        this.dragOffset = Offset.Zero
    }

    fun updateDrag(delta: Offset) {
        dragOffset += delta
    }

    fun currentCenter(): Offset = origin + dragOffset

    fun findDropTarget(): PileId? {
        val point = currentCenter()
        return pileBounds.entries.firstOrNull { (pile, rect) ->
            pile != source && rect.contains(point)
        }?.key
    }

    fun endDrag() {
        source = null
        sourceIndex = 0
        cards = emptyList()
        origin = Offset.Zero
        dragOffset = Offset.Zero
    }
}
