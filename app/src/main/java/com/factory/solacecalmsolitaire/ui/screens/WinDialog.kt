package com.factory.solacecalmsolitaire.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.factory.solacecalmsolitaire.R

@Composable
fun WinDialog(
    moves: Int,
    elapsedSeconds: Int,
    score: Int,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.win_title)) },
        text = {
            Column {
                Text(stringResource(R.string.win_message, moves, formatTime(elapsedSeconds)))
                Text(stringResource(R.string.win_score, score))
            }
        },
        confirmButton = {
            TextButton(onClick = onPlayAgain) { Text(stringResource(R.string.action_play_again)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}
