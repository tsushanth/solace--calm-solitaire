package com.factory.solacecalmsolitaire.data.repository

import com.factory.solacecalmsolitaire.data.local.GameResultDao
import com.factory.solacecalmsolitaire.data.local.GameResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatsRepository(private val dao: GameResultDao) {

    fun observeResults(): Flow<List<GameResultEntity>> = dao.observeAll()

    fun observeSummary(): Flow<StatsSummary> = observeResults().map { results ->
        if (results.isEmpty()) return@map StatsSummary()

        val gamesPlayed = results.size
        val gamesWon = results.count { it.won }
        val winPercentage = (gamesWon * 100) / gamesPlayed
        val bestTime = results.filter { it.won }.minOfOrNull { it.durationSeconds }
        val bestScore = results.maxOf { it.score }

        // results are ordered most-recent-first.
        var currentStreak = 0
        for (result in results) {
            if (result.won) currentStreak++ else break
        }

        var bestStreak = 0
        var running = 0
        for (result in results.asReversed()) {
            running = if (result.won) running + 1 else 0
            bestStreak = maxOf(bestStreak, running)
        }

        StatsSummary(
            gamesPlayed = gamesPlayed,
            gamesWon = gamesWon,
            winPercentage = winPercentage,
            bestTimeSeconds = bestTime,
            bestScore = bestScore,
            currentStreak = currentStreak,
            bestStreak = bestStreak
        )
    }

    suspend fun recordResult(result: GameResultEntity) = dao.insert(result)

    suspend fun clearHistory() = dao.clearAll()
}
