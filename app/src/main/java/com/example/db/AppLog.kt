package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class AppLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: String // INFO, SUCCESS, WARNING, ERROR
)
