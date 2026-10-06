package com.cdp.learningapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    @Query("SELECT * FROM cached_content WHERE path = :path LIMIT 1")
    fun getCachedContentFlow(path: String): Flow<CachedContentEntity?>

    @Query("SELECT * FROM cached_content WHERE path = :path LIMIT 1")
    suspend fun getCachedContent(path: String): CachedContentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: CachedContentEntity)

    @Query("UPDATE cached_content SET isSavedOffline = :saved WHERE path = :path")
    suspend fun updateSavedOfflineStatus(path: String, saved: Boolean)

    @Query("SELECT * FROM cached_content WHERE isSavedOffline = 1")
    fun getAllOfflineSavedContent(): Flow<List<CachedContentEntity>>

    @Query("DELETE FROM cached_content WHERE path = :path")
    suspend fun deleteByPath(path: String)
}

@Dao
interface TestAttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: TestAttemptEntity): Long

    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<TestAttemptEntity>>

    @Query("SELECT * FROM test_attempts WHERE testId = :testId ORDER BY timestamp DESC")
    fun getAttemptsForTest(testId: String): Flow<List<TestAttemptEntity>>
}
