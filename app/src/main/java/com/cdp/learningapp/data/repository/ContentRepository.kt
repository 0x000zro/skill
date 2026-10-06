package com.cdp.learningapp.data.repository

import com.cdp.learningapp.data.local.AppDatabase
import com.cdp.learningapp.data.local.CachedContentEntity
import com.cdp.learningapp.data.local.TestAttemptEntity
import com.cdp.learningapp.data.model.ManifestResponse
import com.cdp.learningapp.data.model.MockTest
import com.cdp.learningapp.data.model.PracticeSet
import com.cdp.learningapp.data.remote.AppConfig
import com.cdp.learningapp.data.remote.GitHubApiService
import com.cdp.learningapp.domain.model.Resource
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.Exception

class ContentRepository(
    private val apiService: GitHubApiService,
    private val database: AppDatabase,
    private val appConfig: AppConfig,
    private val gson: Gson = Gson()
) {
    private val contentDao = database.contentDao()
    private val testAttemptDao = database.testAttemptDao()

    /**
     * Fetch Manifest with offline-first strategy.
     */
    fun getManifest(forceRefresh: Boolean = false): Flow<Resource<ManifestResponse>> = flow {
        emit(Resource.Loading())

        val cachedEntity = contentDao.getCachedContent("manifest.json")
        var cachedManifest: ManifestResponse? = null

        if (cachedEntity != null) {
            try {
                cachedManifest = gson.fromJson(cachedEntity.content, ManifestResponse::class.java)
                emit(Resource.Success(data = cachedManifest, isFromCache = true))
            } catch (e: Exception) {
                // Ignore parse errors from stale cache
            }
        }

        // Fetch from network if forced or cache was empty
        if (forceRefresh || cachedManifest == null) {
            try {
                val manifestUrl = appConfig.getRawFileUrl("manifest.json")
                val remoteManifest = apiService.getManifest(manifestUrl)

                // Cache in Room
                val jsonString = gson.toJson(remoteManifest)
                contentDao.insertOrUpdate(
                    CachedContentEntity(
                        path = "manifest.json",
                        content = jsonString,
                        contentType = "manifest"
                    )
                )

                emit(Resource.Success(data = remoteManifest, isFromCache = false))
            } catch (e: Exception) {
                if (cachedManifest != null) {
                    // We already emitted the cached manifest, but notify of sync error if needed
                    emit(Resource.Success(data = cachedManifest, isFromCache = true))
                } else {
                    emit(Resource.Error(message = e.localizedMessage ?: "Failed to connect to GitHub CMS"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetch Markdown Note (.md) with offline-first Room cache.
     */
    fun getMarkdownNote(relativePath: String, forceRefresh: Boolean = false): Flow<Resource<String>> = flow {
        emit(Resource.Loading())

        val cached = contentDao.getCachedContent(relativePath)
        if (cached != null) {
            emit(Resource.Success(data = cached.content, isFromCache = true))
        }

        if (forceRefresh || cached == null) {
            try {
                val url = appConfig.getRawFileUrl(relativePath)
                val rawMarkdown = apiService.getRawMarkdown(url)

                contentDao.insertOrUpdate(
                    CachedContentEntity(
                        path = relativePath,
                        content = rawMarkdown,
                        contentType = "markdown",
                        isSavedOffline = cached?.isSavedOffline ?: false
                    )
                )

                emit(Resource.Success(data = rawMarkdown, isFromCache = false))
            } catch (e: Exception) {
                if (cached != null) {
                    emit(Resource.Success(data = cached.content, isFromCache = true))
                } else {
                    emit(Resource.Error(message = "Unable to load note: ${e.localizedMessage}"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetch Practice MCQ Set with offline-first Room cache.
     */
    fun getPracticeSet(relativePath: String, forceRefresh: Boolean = false): Flow<Resource<PracticeSet>> = flow {
        emit(Resource.Loading())

        val cached = contentDao.getCachedContent(relativePath)
        var cachedData: PracticeSet? = null

        if (cached != null) {
            try {
                cachedData = gson.fromJson(cached.content, PracticeSet::class.java)
                emit(Resource.Success(data = cachedData, isFromCache = true))
            } catch (_: Exception) {}
        }

        if (forceRefresh || cachedData == null) {
            try {
                val url = appConfig.getRawFileUrl(relativePath)
                val remoteData = apiService.getPracticeSet(url)

                contentDao.insertOrUpdate(
                    CachedContentEntity(
                        path = relativePath,
                        content = gson.toJson(remoteData),
                        contentType = "practice"
                    )
                )

                emit(Resource.Success(data = remoteData, isFromCache = false))
            } catch (e: Exception) {
                if (cachedData != null) {
                    emit(Resource.Success(data = cachedData, isFromCache = true))
                } else {
                    emit(Resource.Error(message = "Unable to load practice set: ${e.localizedMessage}"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Fetch Mock Test with offline-first Room cache.
     */
    fun getMockTest(relativePath: String, forceRefresh: Boolean = false): Flow<Resource<MockTest>> = flow {
        emit(Resource.Loading())

        val cached = contentDao.getCachedContent(relativePath)
        var cachedData: MockTest? = null

        if (cached != null) {
            try {
                cachedData = gson.fromJson(cached.content, MockTest::class.java)
                emit(Resource.Success(data = cachedData, isFromCache = true))
            } catch (_: Exception) {}
        }

        if (forceRefresh || cachedData == null) {
            try {
                val url = appConfig.getRawFileUrl(relativePath)
                val remoteData = apiService.getMockTest(url)

                contentDao.insertOrUpdate(
                    CachedContentEntity(
                        path = relativePath,
                        content = gson.toJson(remoteData),
                        contentType = "test"
                    )
                )

                emit(Resource.Success(data = remoteData, isFromCache = false))
            } catch (e: Exception) {
                if (cachedData != null) {
                    emit(Resource.Success(data = cachedData, isFromCache = true))
                } else {
                    emit(Resource.Error(message = "Unable to load mock test: ${e.localizedMessage}"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun toggleOfflineSaved(path: String, saved: Boolean) {
        contentDao.updateSavedOfflineStatus(path, saved)
    }

    suspend fun isNoteSavedOffline(path: String): Boolean {
        return contentDao.getCachedContent(path)?.isSavedOffline ?: false
    }

    suspend fun saveTestAttempt(attempt: TestAttemptEntity): Long {
        return testAttemptDao.insertAttempt(attempt)
    }

    fun getAllTestAttempts(): Flow<List<TestAttemptEntity>> {
        return testAttemptDao.getAllAttempts()
    }
}
