package com.factory.solacecalmsolitaire.viewmodel

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.factory.solacecalmsolitaire.TestSolaceApplication
import com.factory.solacecalmsolitaire.data.repository.StatsRepository
import com.factory.solacecalmsolitaire.data.repository.StatsSummary
import com.factory.solacecalmsolitaire.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = TestSolaceApplication::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: TestSolaceApplication
    private lateinit var summaryFlow: MutableStateFlow<StatsSummary>
    private lateinit var statsRepository: StatsRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        summaryFlow = MutableStateFlow(StatsSummary())
        statsRepository = mockk(relaxed = true)
        every { statsRepository.observeSummary() } returns summaryFlow
        app.statsRepositoryOverride = statsRepository
    }

    @Test
    fun `initial summary is the default until the repository emits`() = runTest {
        val viewModel = StatsViewModel(app)

        viewModel.summary.test {
            assertEquals(StatsSummary(), awaitItem())
        }
    }

    @Test
    fun `summary reflects repository updates`() = runTest {
        val viewModel = StatsViewModel(app)

        viewModel.summary.test {
            assertEquals(StatsSummary(), awaitItem())

            summaryFlow.value = StatsSummary(gamesPlayed = 5, gamesWon = 3, winPercentage = 60)
            val updated = awaitItem()
            assertEquals(5, updated.gamesPlayed)
            assertEquals(3, updated.gamesWon)
            assertEquals(60, updated.winPercentage)
        }
    }

    @Test
    fun `clearHistory delegates to the repository`() {
        val viewModel = StatsViewModel(app)

        viewModel.clearHistory()

        coVerify { statsRepository.clearHistory() }
    }
}
