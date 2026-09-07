package com.factory.solacecalmsolitaire.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factory.solacecalmsolitaire.R
import com.factory.solacecalmsolitaire.model.Card
import com.factory.solacecalmsolitaire.model.PileId
import com.factory.solacecalmsolitaire.model.Suit
import com.factory.solacecalmsolitaire.ui.theme.CardDimens

/**
 * A card that can be tapped (single tap => try auto-move to foundation) and/or dragged
 * onto another pile. [draggedCards] is the run that travels together when [index] starts a drag.
 */
@Composable
private fun DraggableCard(
    card: Card,
    pileId: PileId,
    index: Int,
    draggedCards: List<Card>,
    dragController: DragController,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDrop: (source: PileId, indexInPile: Int, destination: PileId) -> Unit,
    onDragStart: () -> Unit = {},
    tapPerformsAction: Boolean = false
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val isBeingDragged = dragController.isDragging &&
        dragController.source == pileId &&
        index >= dragController.sourceIndex
    val tapHint = stringResource(R.string.cd_double_tap_to_foundation)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates = it }
            .alpha(if (isBeingDragged) 0f else 1f)
            .semantics(mergeDescendants = true) {
                contentDescription = card.accessibleDescription
                if (tapPerformsAction) {
                    onClick(label = tapHint) { onTap(); true }
                }
            }
            .then(
                if (interactive) {
                    Modifier
                        .pointerInput(pileId, index, card.id) {
                            detectDragGestures(
                                onDragStart = {
                                    val root = coordinates?.positionInRoot() ?: Offset.Zero
                                    dragController.beginDrag(pileId, index, draggedCards, root)
                                    onDragStart()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragController.updateDrag(amount)
                                },
                                onDragEnd = {
                                    val target = dragController.findDropTarget()
                                    if (target != null) onDrop(pileId, index, target)
                                    dragController.endDrag()
                                },
                                onDragCancel = { dragController.endDrag() }
                            )
                        }
                        .pointerInput(pileId, index, card.id) {
                            detectTapGestures(onTap = { onTap() })
                        }
                } else Modifier
            )
    ) {
        PlayingCardView(card = card)
    }
}

@Composable
private fun EmptyPileSlot(
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(width = CardDimens.WIDTH, height = CardDimens.HEIGHT)
            .clip(RoundedCornerShape(CardDimens.CORNER_RADIUS))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(CardDimens.CORNER_RADIUS)
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun StockPileView(
    cards: List<Card>,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val description = if (cards.isEmpty()) {
        stringResource(R.string.cd_stock_pile_empty)
    } else {
        stringResource(R.string.cd_stock_pile_filled, cards.size)
    }
    Box(
        modifier = modifier
            .size(width = CardDimens.WIDTH, height = CardDimens.HEIGHT)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                onClick(label = null) { onTap(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        if (cards.isEmpty()) {
            EmptyPileSlot {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            PlayingCardView(card = cards.last())
        }
    }
}

@Composable
fun WastePileView(
    cards: List<Card>,
    dragController: DragController,
    modifier: Modifier = Modifier,
    onTap: (Int) -> Unit,
    onDrop: (source: PileId, indexInPile: Int, destination: PileId) -> Unit,
    onDragStart: () -> Unit = {}
) {
    val pileId = PileId.Waste
    Box(
        modifier = modifier
            .width(CardDimens.WIDTH + 24.dp)
            .height(CardDimens.HEIGHT)
            .onGloballyPositioned { dragController.pileBounds[pileId] = it.boundsInRoot() }
    ) {
        if (cards.isEmpty()) {
            EmptyPileSlot(contentDescription = stringResource(R.string.cd_waste_pile_empty))
        } else {
            val visible = cards.takeLast(3)
            val firstVisibleGlobalIndex = cards.size - visible.size
            visible.forEachIndexed { offsetIndex, card ->
                val globalIndex = firstVisibleGlobalIndex + offsetIndex
                val isTop = globalIndex == cards.lastIndex
                DraggableCard(
                    card = card,
                    pileId = pileId,
                    index = globalIndex,
                    draggedCards = listOf(card),
                    dragController = dragController,
                    interactive = isTop,
                    modifier = Modifier.offset(x = (offsetIndex * 12).dp),
                    onTap = { onTap(globalIndex) },
                    onDrop = onDrop,
                    onDragStart = onDragStart,
                    tapPerformsAction = isTop
                )
            }
        }
    }
}

@Composable
fun FoundationPileView(
    index: Int,
    cards: List<Card>,
    suitHint: Suit,
    dragController: DragController,
    modifier: Modifier = Modifier,
    onDrop: (source: PileId, indexInPile: Int, destination: PileId) -> Unit,
    onDragStart: () -> Unit = {}
) {
    val pileId = PileId.Foundation(index)
    Box(
        modifier = modifier
            .size(width = CardDimens.WIDTH, height = CardDimens.HEIGHT)
            .onGloballyPositioned { dragController.pileBounds[pileId] = it.boundsInRoot() },
        contentAlignment = Alignment.Center
    ) {
        if (cards.isEmpty()) {
            EmptyPileSlot(contentDescription = stringResource(R.string.cd_foundation_empty, suitHint.fullName)) {
                Text(
                    text = suitHint.symbol,
                    color = if (suitHint.isRed) Color(0xFFC1442E).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.3f),
                    fontSize = 26.sp
                )
            }
        } else {
            DraggableCard(
                card = cards.last(),
                pileId = pileId,
                index = cards.lastIndex,
                draggedCards = listOf(cards.last()),
                dragController = dragController,
                interactive = true,
                onTap = {},
                onDrop = onDrop,
                onDragStart = onDragStart
            )
        }
    }
}

@Composable
fun TableauPileView(
    index: Int,
    cards: List<Card>,
    dragController: DragController,
    modifier: Modifier = Modifier,
    onTap: (Int) -> Unit,
    onDrop: (source: PileId, indexInPile: Int, destination: PileId) -> Unit,
    onDragStart: () -> Unit = {}
) {
    val pileId = PileId.Tableau(index)
    val offsets: List<Dp> = remember(cards) {
        val result = mutableListOf<Dp>()
        var acc = 0.dp
        for (c in cards) {
            result.add(acc)
            acc += if (c.isFaceUp) CardDimens.FACE_UP_OVERLAP else CardDimens.FACE_DOWN_OVERLAP
        }
        result
    }
    val pileHeight = if (offsets.isEmpty()) CardDimens.HEIGHT else offsets.last() + CardDimens.HEIGHT

    Box(
        modifier = modifier
            .width(CardDimens.WIDTH)
            .height(pileHeight)
            .onGloballyPositioned { dragController.pileBounds[pileId] = it.boundsInRoot() }
    ) {
        if (cards.isEmpty()) {
            EmptyPileSlot(contentDescription = stringResource(R.string.cd_tableau_empty))
        } else {
            cards.forEachIndexed { i, card ->
                DraggableCard(
                    card = card,
                    pileId = pileId,
                    index = i,
                    draggedCards = cards.subList(i, cards.size),
                    dragController = dragController,
                    interactive = card.isFaceUp,
                    modifier = Modifier.offset(y = offsets[i]),
                    onTap = { onTap(i) },
                    tapPerformsAction = i == cards.lastIndex,
                    onDrop = onDrop,
                    onDragStart = onDragStart
                )
            }
        }
    }
}
