package com.cdp.learningapp.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.cdp.learningapp.BuildConfig

class AppConfig(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cdp_app_config", Context.MODE_PRIVATE)

    var githubOwner: String
        get() = prefs.getString("github_owner", BuildConfig.DEFAULT_GITHUB_OWNER) ?: BuildConfig.DEFAULT_GITHUB_OWNER
        set(value) = prefs.edit().putString("github_owner", value.trim()).apply()

    var githubRepo: String
        get() = prefs.getString("github_repo", BuildConfig.DEFAULT_GITHUB_REPO) ?: BuildConfig.DEFAULT_GITHUB_REPO
        set(value) = prefs.edit().putString("github_repo", value.trim()).apply()

    var githubBranch: String
        get() = prefs.getString("github_branch", BuildConfig.DEFAULT_GITHUB_BRANCH) ?: BuildConfig.DEFAULT_GITHUB_BRANCH
        set(value) = prefs.edit().putString("github_branch", value.trim()).apply()

    /**
     * Resolves the raw content URL root, e.g.:
     * https://raw.githubusercontent.com/<OWNER>/<REPO>/<BRANCH>/
     */
    val rawBaseUrl: String
        get() = "https://raw.githubusercontent.com/$githubOwner/$githubRepo/$githubBranch/"

    fun getRawFileUrl(relativePath: String): String {
        val cleanPath = relativePath.trimStart('/')
        return "$rawBaseUrl$cleanPath"
    }
}
