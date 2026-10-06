package com.cdp.learningapp

import android.app.Application
import com.cdp.learningapp.data.local.AppDatabase
import com.cdp.learningapp.data.remote.AppConfig
import com.cdp.learningapp.data.remote.NetworkClient
import com.cdp.learningapp.data.repository.ContentRepository

class CDPApplication : Application() {

    lateinit var appConfig: AppConfig
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ContentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        appConfig = AppConfig(this)
        database = AppDatabase.getInstance(this)

        val okHttpClient = NetworkClient.createOkHttpClient(this)
        val apiService = NetworkClient.createRetrofit(okHttpClient)

        repository = ContentRepository(
            apiService = apiService,
            database = database,
            appConfig = appConfig
        )
    }

    companion object {
        lateinit var instance: CDPApplication
            private set
    }
}
