package com.factory.solacecalmsolitaire.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val won: Boolean,
    val drawCount: Int,
    val score: Int,
    val moves: Int,
    val durationSeconds: Int,
    val completedAtEpochMillis: Long
)
