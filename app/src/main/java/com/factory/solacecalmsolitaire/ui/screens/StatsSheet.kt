package com.factory.solacecalmsolitaire.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.factory.solacecalmsolitaire.R
import com.factory.solacecalmsolitaire.data.repository.StatsSummary
import com.factory.solacecalmsolitaire.ui.components.LockIcon
import com.factory.solacecalmsolitaire.ui.components.ProBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(
    summary: StatsSummary,
    isPremium: Boolean,
    hapticsEnabled: Boolean,
    onClear: () -> Unit,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    fun tapFeedback() {
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (summary.gamesPlayed == 0) {
                Text(stringResource(R.string.stats_no_data), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                StatsRow(stringResource(R.string.stats_games_played), summary.gamesPlayed.toString())
                StatsRow(stringResource(R.string.stats_games_won), summary.gamesWon.toString())
                StatsRow(stringResource(R.string.stats_win_percentage), "${summary.winPercentage}%")

                if (isPremium) {
                    StatsRow(
                        stringResource(R.string.stats_best_time),
                        summary.bestTimeSeconds?.let { formatTime(it) } ?: "--:--"
                    )
                    StatsRow(stringResource(R.string.stats_best_score), summary.bestScore.toString())
                    StatsRow(stringResource(R.string.stats_current_streak), summary.currentStreak.toString())
                    StatsRow(stringResource(R.string.stats_best_streak), summary.bestStreak.toString())
                } else {
                    LockedAdvancedStatsRow(onUpgrade = { tapFeedback(); onUpgrade() })
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { tapFeedback(); showClearConfirm = true }) {
                    Text(stringResource(R.string.action_clear_stats))
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.dialog_clear_stats_title)) },
            text = { Text(stringResource(R.string.dialog_clear_stats_message)) },
            confirmButton = {
                TextButton(onClick = {
                    tapFeedback()
                    onClear()
                    showClearConfirm = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun LockedAdvancedStatsRow(onUpgrade: () -> Unit) {
    val label = stringResource(R.string.stats_locked_advanced)
    val lockedDescription = stringResource(R.string.cd_locked_premium)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUpgrade)
            .semantics(mergeDescendants = true) { contentDescription = "$label. $lockedDescription" }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            LockIcon()
        }
        ProBadge()
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
