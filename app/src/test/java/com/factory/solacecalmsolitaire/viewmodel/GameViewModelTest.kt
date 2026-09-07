package com.factory.solacecalmsolitaire.viewmodel

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.factory.solacecalmsolitaire.TestSolaceApplication
import com.factory.solacecalmsolitaire.data.datastore.GameSettings
import com.factory.solacecalmsolitaire.data.datastore.SettingsDataStore
import com.factory.solacecalmsolitaire.data.repository.StatsRepository
import com.factory.solacecalmsolitaire.premium.PremiumManager
import com.factory.solacecalmsolitaire.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [GameViewModel]'s init blocks run on the [MainDispatcherRule]'s eager, unconfined dispatcher,
 * so by the time [createViewModel] returns, the "loading -> settled" transition has already
 * happened synchronously. Every test below observes the already-settled state as its first
 * Turbine item, then drives further state changes explicitly.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = TestSolaceApplication::class)
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: TestSolaceApplication
    private lateinit var settingsFlow: MutableStateFlow<GameSettings>
    private lateinit var isPremium: MutableStateFlow<Boolean>
    private lateinit var isLoaded: MutableStateFlow<Boolean>
    private lateinit var hasSeenPaywall: MutableStateFlow<Boolean>
    private lateinit var statsRepository: StatsRepository

    private fun createViewModel(): GameViewModel = GameViewModel(app)

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()

        settingsFlow = MutableStateFlow(GameSettings())
        val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
        every { settingsDataStore.settings } returns settingsFlow
        app.settingsDataStoreOverride = settingsDataStore

        statsRepository = mockk(relaxed = true)
        app.statsRepositoryOverride = statsRepository

        isPremium = MutableStateFlow(false)
        isLoaded = MutableStateFlow(false)
        hasSeenPaywall = MutableStateFlow(false)
        val premiumManager = mockk<PremiumManager>(relaxed = true)
        every { premiumManager.isPremium } returns isPremium
        every { premiumManager.isLoaded } returns isLoaded
        every { premiumManager.hasSeenInitialPaywall } returns hasSeenPaywall
        app.premiumManagerOverride = premiumManager
    }

    @Test
    fun `initial state has started a new game once construction settles`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0, state.game.moves)
        assertFalse(state.canUndo)
        assertEquals(FREE_UNDO_LIMIT, state.freeUndosRemaining)
    }

    @Test
    fun `onStockTap draws from stock and enables undo`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // settled initial state

            viewModel.onStockTap()

            val afterDraw = awaitItem()
            assertEquals(1, afterDraw.game.moves)
            assertTrue(afterDraw.canUndo)
        }
    }

    @Test
    fun `requestUndo with no history returns NoHistory`() {
        val viewModel = createViewModel()
        assertEquals(UndoOutcome.NoHistory, viewModel.requestUndo())
    }

    @Test
    fun `free users are capped at FREE_UNDO_LIMIT undos per game`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // settled initial state

            repeat(FREE_UNDO_LIMIT + 1) {
                viewModel.onStockTap()
                awaitItem()
            }

            repeat(FREE_UNDO_LIMIT) {
                assertEquals(UndoOutcome.Performed, viewModel.requestUndo())
                awaitItem()
            }
            assertEquals(0, viewModel.uiState.value.freeUndosRemaining)
            assertEquals(UndoOutcome.LimitReached, viewModel.requestUndo())
        }
    }

    @Test
    fun `premium users have unlimited undos`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // settled initial state

            isPremium.value = true
            assertTrue(awaitItem().isPremium)

            repeat(FREE_UNDO_LIMIT + 2) {
                viewModel.onStockTap()
                awaitItem()
            }
            repeat(FREE_UNDO_LIMIT + 2) {
                assertEquals(UndoOutcome.Performed, viewModel.requestUndo())
                awaitItem()
            }
            assertEquals(FREE_UNDO_LIMIT, viewModel.uiState.value.freeUndosRemaining)
        }
    }

    @Test
    fun `startNewGame resets undo history and free undo count`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // settled initial state

            viewModel.onStockTap()
            awaitItem()
            viewModel.requestUndo()
            awaitItem()

            viewModel.startNewGame()
            val afterNewGame = awaitItem()
            assertFalse(afterNewGame.canUndo)
            assertEquals(FREE_UNDO_LIMIT, afterNewGame.freeUndosRemaining)
        }
    }

    @Test
    fun `first launch paywall shows for free users who have not seen it`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // settled initial state

            isLoaded.value = true
            val withPaywall = awaitItem()
            assertTrue(withPaywall.showFirstLaunchPaywall)
        }
    }

    @Test
    fun `first launch paywall does not show once already seen`() = runTest {
        hasSeenPaywall.value = true
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.showFirstLaunchPaywall)

            isLoaded.value = true
            expectNoEvents()
        }
    }

    @Test
    fun `first launch paywall does not show for premium users`() = runTest {
        isPremium.value = true
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isPremium)

            isLoaded.value = true
            expectNoEvents()
        }
    }

    @Test
    fun `consumeFirstLaunchPaywallFlag hides paywall and persists the flag`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem() // settled initial state
            isLoaded.value = true
            assertTrue(awaitItem().showFirstLaunchPaywall)

            viewModel.consumeFirstLaunchPaywallFlag()
            assertFalse(awaitItem().showFirstLaunchPaywall)
        }
        coVerify { app.premiumManager.markInitialPaywallSeen() }
    }

    @Test
    fun `setSoundEnabled delegates to settings data store`() {
        val viewModel = createViewModel()

        viewModel.setSoundEnabled(false)

        coVerify { app.settingsDataStore.setSoundEnabled(false) }
    }

    @Test
    fun `setDrawCount delegates to settings data store`() {
        val viewModel = createViewModel()

        viewModel.setDrawCount(1)

        coVerify { app.settingsDataStore.setDrawCount(1) }
    }
}
