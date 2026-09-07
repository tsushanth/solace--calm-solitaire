package com.factory.solacecalmsolitaire.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameResultDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: GameResultDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.gameResultDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun result(
        won: Boolean,
        drawCount: Int = 3,
        score: Int = 100,
        moves: Int = 20,
        durationSeconds: Int = 60,
        completedAtEpochMillis: Long = 1_000L
    ) = GameResultEntity(
        won = won,
        drawCount = drawCount,
        score = score,
        moves = moves,
        durationSeconds = durationSeconds,
        completedAtEpochMillis = completedAtEpochMillis
    )

    @Test
    fun `insert then observeAll returns the inserted result`() = runTest {
        dao.insert(result(won = true))

        dao.observeAll().test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertTrue(results.first().won)
        }
    }

    @Test
    fun `observeAll orders results most-recent-first`() = runTest {
        dao.insert(result(won = true, completedAtEpochMillis = 1_000L))
        dao.insert(result(won = false, completedAtEpochMillis = 3_000L))
        dao.insert(result(won = true, completedAtEpochMillis = 2_000L))

        dao.observeAll().test {
            val results = awaitItem()
            assertEquals(listOf(3_000L, 2_000L, 1_000L), results.map { it.completedAtEpochMillis })
        }
    }

    @Test
    fun `observeAll emits a new list after each insert`() = runTest {
        dao.observeAll().test {
            assertEquals(0, awaitItem().size)
            dao.insert(result(won = true))
            assertEquals(1, awaitItem().size)
            dao.insert(result(won = false))
            assertEquals(2, awaitItem().size)
        }
    }

    @Test
    fun `clearAll removes every row`() = runTest {
        dao.insert(result(won = true))
        dao.insert(result(won = false))

        dao.clearAll()

        dao.observeAll().test {
            assertEquals(0, awaitItem().size)
        }
    }

    @Test
    fun `insert autogenerates a primary key per row`() = runTest {
        dao.insert(result(won = true))
        dao.insert(result(won = true))

        dao.observeAll().test {
            val results = awaitItem()
            assertEquals(2, results.map { it.id }.distinct().size)
        }
    }
}
