package com.cdp.learningapp.data.remote

import com.cdp.learningapp.data.model.ManifestResponse
import com.cdp.learningapp.data.model.MockTest
import com.cdp.learningapp.data.model.PracticeSet
import retrofit2.http.GET
import retrofit2.http.Url

interface GitHubApiService {

    @GET
    suspend fun getManifest(@Url url: String): ManifestResponse

    @GET
    suspend fun getRawMarkdown(@Url url: String): String

    @GET
    suspend fun getPracticeSet(@Url url: String): PracticeSet

    @GET
    suspend fun getMockTest(@Url url: String): MockTest
}
