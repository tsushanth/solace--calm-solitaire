package com.factory.solacecalmsolitaire.viewmodel

import com.factory.solacecalmsolitaire.engine.GameState
import com.factory.solacecalmsolitaire.ui.theme.FeltTheme

/** Free users get a limited number of undos per game; premium users get unlimited. */
const val FREE_UNDO_LIMIT = 3

data class GameUiState(
    val game: GameState = GameState(),
    val canUndo: Boolean = false,
    val canAutoComplete: Boolean = false,
    val isAutoCompleting: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoCompleteEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val freeUndosRemaining: Int = FREE_UNDO_LIMIT,
    val feltTheme: FeltTheme = FeltTheme.CLASSIC,
    val dynamicColorEnabled: Boolean = true,
    val showFirstLaunchPaywall: Boolean = false
)

sealed class UndoOutcome {
    data object Performed : UndoOutcome()
    data object LimitReached : UndoOutcome()
    data object NoHistory : UndoOutcome()
}
