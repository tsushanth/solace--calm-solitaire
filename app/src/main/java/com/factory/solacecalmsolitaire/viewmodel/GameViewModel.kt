package com.factory.solacecalmsolitaire.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.solacecalmsolitaire.SolaceApplication
import com.factory.solacecalmsolitaire.data.local.GameResultEntity
import com.factory.solacecalmsolitaire.data.repository.StatsRepository
import com.factory.solacecalmsolitaire.data.datastore.SettingsDataStore
import com.factory.solacecalmsolitaire.engine.GameState
import com.factory.solacecalmsolitaire.engine.SolitaireEngine
import com.factory.solacecalmsolitaire.model.PileId
import com.factory.solacecalmsolitaire.premium.PremiumManager
import com.factory.solacecalmsolitaire.ui.theme.FeltTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore: SettingsDataStore =
        (application as SolaceApplication).settingsDataStore
    private val statsRepository: StatsRepository =
        (application as SolaceApplication).statsRepository
    private val premiumManager: PremiumManager =
        (application as SolaceApplication).premiumManager

    private val history = ArrayDeque<GameState>()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var hasRecordedResult = false
    private var hasEvaluatedFirstLaunchPaywall = false

    init {
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        soundEnabled = settings.soundEnabled,
                        hapticsEnabled = settings.hapticsEnabled,
                        autoCompleteEnabled = settings.autoCompleteEnabled,
                        feltTheme = settings.feltTheme,
                        dynamicColorEnabled = settings.dynamicColorEnabled
                    )
                }
                if (_uiState.value.isLoading) {
                    startNewGame(settings.drawCount)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
        viewModelScope.launch {
            combine(
                premiumManager.isPremium,
                premiumManager.isLoaded,
                premiumManager.hasSeenInitialPaywall
            ) { isPremium, isLoaded, hasSeenPaywall -> Triple(isPremium, isLoaded, hasSeenPaywall) }
                .collect { (isPremium, isLoaded, hasSeenPaywall) ->
                    _uiState.update { it.copy(isPremium = isPremium) }
                    if (isLoaded && !hasEvaluatedFirstLaunchPaywall) {
                        hasEvaluatedFirstLaunchPaywall = true
                        if (!isPremium && !hasSeenPaywall) {
                            _uiState.update { it.copy(showFirstLaunchPaywall = true) }
                        }
                    }
                }
        }
    }

    fun startNewGame(drawCount: Int = _uiState.value.game.drawCount) {
        recordLossIfUnfinished()
        history.clear()
        hasRecordedResult = false
        val newGame = SolitaireEngine.newGame(drawCount)
        _uiState.update {
            it.copy(
                game = newGame,
                canUndo = false,
                canAutoComplete = false,
                isAutoCompleting = false,
                freeUndosRemaining = FREE_UNDO_LIMIT
            )
        }
        startTimer()
    }

    fun consumeFirstLaunchPaywallFlag() {
        _uiState.update { it.copy(showFirstLaunchPaywall = false) }
        viewModelScope.launch { premiumManager.markInitialPaywallSeen() }
    }

    fun onStockTap() {
        val state = _uiState.value.game
        if (!SolitaireEngine.canDrawFromStock(state)) return
        applyIfChanged(SolitaireEngine.drawFromStock(state))
    }

    /** Single tap on a top card tries to send it straight to a foundation. */
    fun onCardTap(source: PileId, indexInPile: Int) {
        val state = _uiState.value.game
        when (source) {
            is PileId.Waste ->
                if (indexInPile == state.waste.lastIndex) {
                    applyIfChanged(SolitaireEngine.moveWasteToFoundation(state))
                }
            is PileId.Tableau ->
                if (indexInPile == state.tableau[source.index].lastIndex) {
                    applyIfChanged(SolitaireEngine.moveTableauToFoundation(state, source.index))
                }
            else -> Unit
        }
    }

    /** Explicit drag-and-drop move; returns true if the move was legal and applied. */
    fun attemptMove(source: PileId, indexInPile: Int, destination: PileId): Boolean {
        val state = _uiState.value.game
        val newState = when {
            source is PileId.Waste && destination is PileId.Foundation ->
                SolitaireEngine.moveWasteToFoundation(state)
            source is PileId.Waste && destination is PileId.Tableau ->
                SolitaireEngine.moveWasteToTableau(state, destination.index)
            source is PileId.Tableau && destination is PileId.Foundation ->
                if (indexInPile == state.tableau[source.index].lastIndex) {
                    SolitaireEngine.moveTableauToFoundation(state, source.index)
                } else null
            source is PileId.Tableau && destination is PileId.Tableau ->
                SolitaireEngine.moveTableauToTableau(state, source.index, indexInPile, destination.index)
            source is PileId.Foundation && destination is PileId.Tableau ->
                SolitaireEngine.moveFoundationToTableau(state, source.index, destination.index)
            else -> null
        }
        val changed = newState != null
        applyIfChanged(newState)
        return changed
    }

    /** Free users are capped at [FREE_UNDO_LIMIT] undos per game; premium users are unlimited. */
    fun requestUndo(): UndoOutcome {
        if (history.isEmpty()) return UndoOutcome.NoHistory
        val state = _uiState.value
        if (!state.isPremium && state.freeUndosRemaining <= 0) return UndoOutcome.LimitReached

        val previous = history.removeLast()
        _uiState.update {
            it.copy(
                game = previous,
                canUndo = history.isNotEmpty(),
                canAutoComplete = SolitaireEngine.canAutoComplete(previous),
                freeUndosRemaining = if (it.isPremium) it.freeUndosRemaining else (it.freeUndosRemaining - 1).coerceAtLeast(0)
            )
        }
        return UndoOutcome.Performed
    }

    fun autoComplete() {
        if (_uiState.value.isAutoCompleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAutoCompleting = true) }
            while (true) {
                val current = _uiState.value.game
                val next = SolitaireEngine.autoCompleteOneStep(current) ?: break
                history.addLast(current)
                _uiState.update { it.copy(game = next, canUndo = true) }
                checkWin(next)
                delay(120)
            }
            _uiState.update { it.copy(isAutoCompleting = false, canAutoComplete = false) }
        }
    }

    fun setDrawCount(drawCount: Int) {
        viewModelScope.launch { settingsDataStore.setDrawCount(drawCount) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSoundEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setHapticsEnabled(enabled) }
    }

    fun setAutoCompleteEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoCompleteEnabled(enabled) }
    }

    fun setFeltTheme(theme: FeltTheme) {
        viewModelScope.launch { settingsDataStore.setFeltTheme(theme) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicColorEnabled(enabled) }
    }

    private fun applyIfChanged(newState: GameState?) {
        if (newState == null) return
        history.addLast(_uiState.value.game)
        _uiState.update {
            it.copy(
                game = newState,
                canUndo = true,
                canAutoComplete = SolitaireEngine.canAutoComplete(newState)
            )
        }
        checkWin(newState)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { current ->
                    if (current.game.isWon) current
                    else current.copy(game = current.game.copy(elapsedSeconds = current.game.elapsedSeconds + 1))
                }
            }
        }
    }

    private fun checkWin(state: GameState) {
        if (state.isWon && !hasRecordedResult) {
            hasRecordedResult = true
            timerJob?.cancel()
            persistResult(won = true, state = state)
        }
    }

    private fun recordLossIfUnfinished() {
        val state = _uiState.value.game
        if (!hasRecordedResult && !state.isWon && state.moves > 0) {
            hasRecordedResult = true
            persistResult(won = false, state = state)
        }
    }

    private fun persistResult(won: Boolean, state: GameState) {
        viewModelScope.launch {
            statsRepository.recordResult(
                GameResultEntity(
                    won = won,
                    drawCount = state.drawCount,
                    score = state.score,
                    moves = state.moves,
                    durationSeconds = state.elapsedSeconds,
                    completedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
