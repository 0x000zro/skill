package com.cdp.learningapp.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    
    object MarkdownReader : Screen("markdown_reader?path={path}&title={title}") {
        fun createRoute(path: String, title: String): String {
            val encPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            val encTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "markdown_reader?path=$encPath&title=$encTitle"
        }
    }

    object PracticeQuiz : Screen("practice_quiz?path={path}&title={title}") {
        fun createRoute(path: String, title: String): String {
            val encPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            val encTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "practice_quiz?path=$encPath&title=$encTitle"
        }
    }

    object MockTest : Screen("mock_test?path={path}&title={title}") {
        fun createRoute(path: String, title: String): String {
            val encPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
            val encTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            return "mock_test?path=$encPath&title=$encTitle"
        }
    }
}
