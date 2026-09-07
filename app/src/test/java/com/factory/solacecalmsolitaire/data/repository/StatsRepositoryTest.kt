package com.factory.solacecalmsolitaire.data.repository

import app.cash.turbine.test
import com.factory.solacecalmsolitaire.data.local.GameResultDao
import com.factory.solacecalmsolitaire.data.local.GameResultEntity
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsRepositoryTest {

    private fun entity(
        won: Boolean,
        score: Int = 0,
        durationSeconds: Int = 0
    ) = GameResultEntity(
        won = won,
        drawCount = 3,
        score = score,
        moves = 10,
        durationSeconds = durationSeconds,
        completedAtEpochMillis = 0L
    )

    @Test
    fun `observeSummary returns defaults for empty history`() = runTest {
        val dao = mockk<GameResultDao>()
        every { dao.observeAll() } returns MutableStateFlow(emptyList())
        val repository = StatsRepository(dao)

        repository.observeSummary().test {
            assertEquals(StatsSummary(), awaitItem())
        }
    }

    @Test
    fun `observeSummary computes win percentage games and best score`() = runTest {
        val dao = mockk<GameResultDao>()
        // Most-recent-first, matching real DAO ordering.
        every { dao.observeAll() } returns MutableStateFlow(
            listOf(
                entity(won = true, score = 500, durationSeconds = 120),
                entity(won = false, score = 100),
                entity(won = true, score = 300, durationSeconds = 90)
            )
        )
        val repository = StatsRepository(dao)

        repository.observeSummary().test {
            val summary = awaitItem()
            assertEquals(3, summary.gamesPlayed)
            assertEquals(2, summary.gamesWon)
            assertEquals(66, summary.winPercentage)
            assertEquals(500, summary.bestScore)
            assertEquals(90, summary.bestTimeSeconds)
        }
    }

    @Test
    fun `observeSummary computes current streak from most recent results`() = runTest {
        val dao = mockk<GameResultDao>()
        every { dao.observeAll() } returns MutableStateFlow(
            listOf(entity(won = true), entity(won = true), entity(won = false), entity(won = true))
        )
        val repository = StatsRepository(dao)

        repository.observeSummary().test {
            assertEquals(2, awaitItem().currentStreak)
        }
    }

    @Test
    fun `observeSummary computes best streak across all results`() = runTest {
        val dao = mockk<GameResultDao>()
        every { dao.observeAll() } returns MutableStateFlow(
            // most-recent-first: loss, win, win, win, loss, win
            listOf(
                entity(won = false),
                entity(won = true),
                entity(won = true),
                entity(won = true),
                entity(won = false),
                entity(won = true)
            )
        )
        val repository = StatsRepository(dao)

        repository.observeSummary().test {
            assertEquals(3, awaitItem().bestStreak)
        }
    }

    @Test
    fun `observeSummary reports null best time when no wins`() = runTest {
        val dao = mockk<GameResultDao>()
        every { dao.observeAll() } returns MutableStateFlow(listOf(entity(won = false, durationSeconds = 45)))
        val repository = StatsRepository(dao)

        repository.observeSummary().test {
            assertNull(awaitItem().bestTimeSeconds)
        }
    }

    @Test
    fun `recordResult delegates to dao insert`() = runTest {
        val dao = mockk<GameResultDao>(relaxed = true)
        val repository = StatsRepository(dao)
        val result = entity(won = true)

        repository.recordResult(result)

        coVerify { dao.insert(result) }
    }

    @Test
    fun `clearHistory delegates to dao clearAll`() = runTest {
        val dao = mockk<GameResultDao>(relaxed = true)
        val repository = StatsRepository(dao)

        repository.clearHistory()

        coVerify { dao.clearAll() }
    }
}
