package com.factory.solacecalmsolitaire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small "PRO" pill used to mark a premium-gated feature in place. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Text(
        text = "PRO",
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 10.sp,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Small lock glyph for use alongside a locked control (e.g. a disabled radio option). */
@Composable
fun LockIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        tint = tint,
        modifier = modifier.padding(start = 4.dp)
    )
}
