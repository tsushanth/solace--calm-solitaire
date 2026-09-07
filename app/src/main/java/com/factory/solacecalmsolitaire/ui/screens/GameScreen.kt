package com.factory.solacecalmsolitaire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factory.solacecalmsolitaire.R
import com.factory.solacecalmsolitaire.model.PileId
import com.factory.solacecalmsolitaire.model.Suit
import com.factory.solacecalmsolitaire.premium.PaywallTrigger
import com.factory.solacecalmsolitaire.ui.components.DragController
import com.factory.solacecalmsolitaire.ui.components.FoundationPileView
import com.factory.solacecalmsolitaire.ui.components.PlayingCardView
import com.factory.solacecalmsolitaire.ui.components.StockPileView
import com.factory.solacecalmsolitaire.ui.components.TableauPileView
import com.factory.solacecalmsolitaire.ui.components.WastePileView
import com.factory.solacecalmsolitaire.ui.theme.CardDimens
import com.factory.solacecalmsolitaire.ui.theme.FeltTheme
import com.factory.solacecalmsolitaire.util.SoundPlayer
import com.factory.solacecalmsolitaire.viewmodel.GameViewModel
import com.factory.solacecalmsolitaire.viewmodel.StatsViewModel
import com.factory.solacecalmsolitaire.viewmodel.UndoOutcome
import kotlin.math.roundToInt

fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = viewModel(),
    statsViewModel: StatsViewModel = viewModel()
) {
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val summary by statsViewModel.summary.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val dragController = remember { DragController() }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }

    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showNewGameConfirm by remember { mutableStateOf(false) }
    var winDialogDismissed by remember { mutableStateOf(false) }
    var paywallTrigger by remember { mutableStateOf<PaywallTrigger?>(null) }

    val game = uiState.game

    LaunchedEffect(uiState.showFirstLaunchPaywall) {
        if (uiState.showFirstLaunchPaywall) {
            paywallTrigger = PaywallTrigger.FIRST_LAUNCH
            gameViewModel.consumeFirstLaunchPaywallFlag()
        }
    }

    LaunchedEffect(game.isWon) {
        if (game.isWon && uiState.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun tapFeedback() {
        if (uiState.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun handleUndo() {
        when (gameViewModel.requestUndo()) {
            UndoOutcome.LimitReached -> paywallTrigger = PaywallTrigger.UNLIMITED_UNDO
            UndoOutcome.Performed -> tapFeedback()
            UndoOutcome.NoHistory -> Unit
        }
    }

    fun handleAutoComplete() {
        if (uiState.isPremium) {
            gameViewModel.autoComplete()
        } else {
            paywallTrigger = PaywallTrigger.AUTO_COMPLETE
        }
    }

    fun handleDrawCountChange(count: Int) {
        if (count == 1 && !uiState.isPremium) {
            paywallTrigger = PaywallTrigger.DRAW_ONE_MODE
        } else {
            gameViewModel.setDrawCount(count)
        }
    }

    fun handleFeltThemeChange(theme: FeltTheme) {
        if (theme.isPremium && !uiState.isPremium) {
            paywallTrigger = PaywallTrigger.FELT_THEME
        } else {
            gameViewModel.setFeltTheme(theme)
        }
    }

    fun feedback(success: Boolean) {
        if (success && uiState.hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (uiState.soundEnabled) {
            if (success) SoundPlayer.playMove() else SoundPlayer.playInvalid()
        }
    }

    fun handleDrop(source: PileId, indexInPile: Int, destination: PileId) {
        val moved = gameViewModel.attemptMove(source, indexInPile, destination)
        feedback(moved)
    }

    fun handleDragStart() {
        if (uiState.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun handleTap(source: PileId, indexInPile: Int) {
        val movesBefore = gameViewModel.uiState.value.game.moves
        gameViewModel.onCardTap(source, indexInPile)
        if (gameViewModel.uiState.value.game.moves != movesBefore) feedback(true)
    }

    fun handleStockTap() {
        val movesBefore = gameViewModel.uiState.value.game.moves
        gameViewModel.onStockTap()
        if (gameViewModel.uiState.value.game.moves != movesBefore) feedback(true)
    }

    fun requestNewGame() {
        tapFeedback()
        if (game.moves > 0 && !game.isWon) {
            showNewGameConfirm = true
        } else {
            winDialogDismissed = false
            gameViewModel.startNewGame()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiState.feltTheme.color)
            .onGloballyPositioned { boardOrigin = it.positionInRoot() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBar(
                modifier = Modifier.statusBarsPadding(),
                score = game.score,
                moves = game.moves,
                elapsedSeconds = game.elapsedSeconds,
                canUndo = uiState.canUndo,
                canAutoComplete = uiState.canAutoComplete && uiState.autoCompleteEnabled,
                onNewGame = ::requestNewGame,
                onUndo = ::handleUndo,
                onAutoComplete = ::handleAutoComplete,
                onStats = { showStats = true },
                onSettings = { showSettings = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                StockPileView(cards = game.stock, onTap = ::handleStockTap)
                WastePileView(
                    cards = game.waste,
                    dragController = dragController,
                    onTap = { index -> handleTap(PileId.Waste, index) },
                    onDrop = ::handleDrop,
                    onDragStart = ::handleDragStart
                )
                Spacer(modifier = Modifier.weight(1f))
                for (i in 0 until 4) {
                    FoundationPileView(
                        index = i,
                        cards = game.foundations[i],
                        suitHint = Suit.entries[i],
                        dragController = dragController,
                        onDrop = ::handleDrop,
                        onDragStart = ::handleDragStart
                    )
                    if (i != 3) Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                for (i in 0 until 7) {
                    TableauPileView(
                        index = i,
                        cards = game.tableau[i],
                        dragController = dragController,
                        onTap = { cardIndex -> handleTap(PileId.Tableau(i), cardIndex) },
                        onDrop = ::handleDrop,
                        onDragStart = ::handleDragStart
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (dragController.isDragging) {
            val local = dragController.currentCenter() - boardOrigin
            Column(
                modifier = Modifier
                    .offset {
                        IntOffset(local.x.roundToInt(), local.y.roundToInt())
                    }
                    // This is a visual echo of the card(s) already exposed via the source pile;
                    // exposing it too would give screen readers a duplicate, unmovable node.
                    .clearAndSetSemantics {}
            ) {
                dragController.cards.forEachIndexed { i, card ->
                    PlayingCardView(
                        card = card,
                        highlighted = true,
                        modifier = if (i == 0) Modifier else Modifier.offset(y = (i * CardDimens.FACE_UP_OVERLAP.value).dp)
                    )
                }
            }
        }

        if (game.isWon && !winDialogDismissed) {
            WinDialog(
                moves = game.moves,
                elapsedSeconds = game.elapsedSeconds,
                score = game.score,
                onPlayAgain = {
                    winDialogDismissed = false
                    gameViewModel.startNewGame()
                },
                onDismiss = { winDialogDismissed = true }
            )
        }

        if (showNewGameConfirm) {
            AlertDialog(
                onDismissRequest = { showNewGameConfirm = false },
                title = { Text(stringResource(R.string.dialog_new_game_title)) },
                text = { Text(stringResource(R.string.dialog_new_game_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        tapFeedback()
                        showNewGameConfirm = false
                        winDialogDismissed = false
                        gameViewModel.startNewGame()
                    }) { Text(stringResource(R.string.action_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showNewGameConfirm = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            drawCount = game.drawCount,
            soundEnabled = uiState.soundEnabled,
            hapticsEnabled = uiState.hapticsEnabled,
            autoCompleteEnabled = uiState.autoCompleteEnabled,
            isPremium = uiState.isPremium,
            feltTheme = uiState.feltTheme,
            dynamicColorEnabled = uiState.dynamicColorEnabled,
            onDrawCountChange = ::handleDrawCountChange,
            onSoundChange = gameViewModel::setSoundEnabled,
            onHapticsChange = gameViewModel::setHapticsEnabled,
            onAutoCompleteChange = gameViewModel::setAutoCompleteEnabled,
            onFeltThemeChange = ::handleFeltThemeChange,
            onDynamicColorChange = gameViewModel::setDynamicColorEnabled,
            onUpgrade = { paywallTrigger = PaywallTrigger.SETTINGS_UPGRADE },
            onDismiss = { showSettings = false }
        )
    }

    if (showStats) {
        StatsSheet(
            summary = summary,
            isPremium = uiState.isPremium,
            hapticsEnabled = uiState.hapticsEnabled,
            onClear = statsViewModel::clearHistory,
            onUpgrade = { paywallTrigger = PaywallTrigger.ADVANCED_STATISTICS },
            onDismiss = { showStats = false }
        )
    }

    paywallTrigger?.let { trigger ->
        PaywallScreen(
            trigger = trigger,
            hapticsEnabled = uiState.hapticsEnabled,
            onClose = { paywallTrigger = null }
        )
    }
}

@Composable
private fun GameTopBar(
    score: Int,
    moves: Int,
    elapsedSeconds: Int,
    canUndo: Boolean,
    canAutoComplete: Boolean,
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onAutoComplete: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNewGame) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_new_game))
        }
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.action_undo))
        }
        if (canAutoComplete) {
            IconButton(onClick = onAutoComplete) {
                Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.action_auto_complete))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        StatChip(label = stringResource(R.string.stat_score), value = score.toString())
        StatChip(label = stringResource(R.string.stat_moves), value = moves.toString())
        StatChip(label = stringResource(R.string.stat_time), value = formatTime(elapsedSeconds))

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onStats) {
            Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.action_statistics))
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    }
}
