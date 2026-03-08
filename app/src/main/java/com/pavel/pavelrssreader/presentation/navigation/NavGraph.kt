package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pavel.pavelrssreader.presentation.articles.ArticleListScreen
import com.pavel.pavelrssreader.presentation.favourites.FavouritesScreen
import com.pavel.pavelrssreader.presentation.feeds.FeedsScreen
import com.pavel.pavelrssreader.presentation.webview.WebViewScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.Articles.route) {
        composable(NavRoutes.Articles.route) {
            ArticleListScreen(onArticleClick = { articleId ->
                navController.navigate(NavRoutes.WebView.createRoute(articleId))
            })
        }
        composable(NavRoutes.Favourites.route) {
            FavouritesScreen(onArticleClick = { articleId ->
                navController.navigate(NavRoutes.WebView.createRoute(articleId))
            })
        }
        composable(NavRoutes.Feeds.route) {
            FeedsScreen()
        }
        composable(
            route = NavRoutes.WebView.route,
            arguments = listOf(navArgument("articleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            WebViewScreen(articleId = articleId, onBack = { navController.popBackStack() })
        }
    }
}
