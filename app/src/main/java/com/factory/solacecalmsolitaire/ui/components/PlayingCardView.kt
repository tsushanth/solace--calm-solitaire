package com.factory.solacecalmsolitaire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factory.solacecalmsolitaire.model.Card
import com.factory.solacecalmsolitaire.ui.theme.CardBack
import com.factory.solacecalmsolitaire.ui.theme.CardBackAccent
import com.factory.solacecalmsolitaire.ui.theme.CardBlack
import com.factory.solacecalmsolitaire.ui.theme.CardDimens
import com.factory.solacecalmsolitaire.ui.theme.CardRed
import com.factory.solacecalmsolitaire.ui.theme.CardWhite

@Composable
fun PlayingCardView(
    card: Card,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    if (card.isFaceUp) {
        CardFace(card = card, highlighted = highlighted, modifier = modifier)
    } else {
        CardBackView(modifier = modifier)
    }
}

@Composable
private fun CardFace(card: Card, highlighted: Boolean, modifier: Modifier = Modifier) {
    val suitColor = if (card.isRed) CardRed else CardBlack
    Box(
        modifier = modifier
            .size(width = CardDimens.WIDTH, height = CardDimens.HEIGHT)
            .clip(RoundedCornerShape(CardDimens.CORNER_RADIUS))
            .background(CardWhite)
            .border(
                width = if (highlighted) 2.dp else CardDimens.BORDER_WIDTH,
                color = if (highlighted) CardBackAccent else Color(0xFFB9B2A0),
                shape = RoundedCornerShape(CardDimens.CORNER_RADIUS)
            )
    ) {
        Text(
            text = "${card.rank.label}${card.suit.symbol}",
            color = suitColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 2.dp)
        )
        Text(
            text = card.suit.symbol,
            color = suitColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = "${card.rank.label}${card.suit.symbol}",
            color = suitColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 2.dp)
                .rotate(180f)
        )
    }
}

@Composable
private fun CardBackView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = CardDimens.WIDTH, height = CardDimens.HEIGHT)
            .clip(RoundedCornerShape(CardDimens.CORNER_RADIUS))
            .background(CardBack)
            .border(
                width = CardDimens.BORDER_WIDTH,
                color = Color(0xFF1B4E63),
                shape = RoundedCornerShape(CardDimens.CORNER_RADIUS)
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CardBackAccent)
                .border(1.dp, Color(0xFF1B4E63), RoundedCornerShape(4.dp))
        )
    }
}
