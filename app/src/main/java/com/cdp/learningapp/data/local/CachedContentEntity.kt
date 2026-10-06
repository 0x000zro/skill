package com.cdp.learningapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_content")
data class CachedContentEntity(
    @PrimaryKey
    val path: String,
    val content: String,
    val contentType: String, // "manifest", "markdown", "practice", "test"
    val lastCachedAt: Long = System.currentTimeMillis(),
    val isSavedOffline: Boolean = false
)

@Entity(tableName = "test_attempts")
data class TestAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testId: String,
    val testTitle: String,
    val score: Int,
    val total: Int,
    val timeTakenSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)
