package com.factory.solacecalmsolitaire.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameResultDao {
    @Insert
    suspend fun insert(result: GameResultEntity)

    @Query("SELECT * FROM game_results ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<GameResultEntity>>

    @Query("DELETE FROM game_results")
    suspend fun clearAll()
}
