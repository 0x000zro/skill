package com.cdp.learningapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cdp.learningapp.ui.home.HomeScreen
import com.cdp.learningapp.ui.markdown.MarkdownReaderScreen
import com.cdp.learningapp.ui.practice.PracticeQuizScreen
import com.cdp.learningapp.ui.test.MockTestScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMarkdown = { path, title ->
                    navController.navigate(Screen.MarkdownReader.createRoute(path, title))
                },
                onNavigateToPractice = { path, title ->
                    navController.navigate(Screen.PracticeQuiz.createRoute(path, title))
                },
                onNavigateToTest = { path, title ->
                    navController.navigate(Screen.MockTest.createRoute(path, title))
                }
            )
        }

        composable(
            route = Screen.MarkdownReader.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
            val path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())

            MarkdownReaderScreen(
                notePath = path,
                noteTitle = title,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInternalNote = { targetPath, targetTitle ->
                    navController.navigate(Screen.MarkdownReader.createRoute(targetPath, targetTitle))
                }
            )
        }

        composable(
            route = Screen.PracticeQuiz.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
            val path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())

            PracticeQuizScreen(
                quizPath = path,
                quizTitle = title,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MockTest.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
            val path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())

            MockTestScreen(
                testPath = path,
                testTitle = title,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
