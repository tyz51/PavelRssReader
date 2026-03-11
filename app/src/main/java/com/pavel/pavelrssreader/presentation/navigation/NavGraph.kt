package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pavel.pavelrssreader.presentation.articles.ArticleListScreen
import com.pavel.pavelrssreader.presentation.favourites.FavouritesScreen
import com.pavel.pavelrssreader.presentation.feeds.FeedsScreen
import com.pavel.pavelrssreader.presentation.settings.FontSizeScreen
import com.pavel.pavelrssreader.presentation.settings.SettingsScreen
import com.pavel.pavelrssreader.presentation.webview.WebViewScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = NavRoutes.Articles.route, modifier = modifier) {
        composable(NavRoutes.Articles.route) {
            ArticleListScreen(onArticleClick = { articleId, feedId ->
                navController.navigate(NavRoutes.WebView.createRoute(articleId, feedId ?: 0L))
            })
        }
        composable(NavRoutes.Favourites.route) {
            FavouritesScreen(onArticleClick = { articleId ->
                navController.navigate(NavRoutes.WebView.createRoute(articleId, 0L))
            })
        }
        composable(NavRoutes.Feeds.route) {
            FeedsScreen()
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(onNavigateToFontSize = {
                navController.navigate(NavRoutes.FontSize.route)
            })
        }
        composable(NavRoutes.FontSize.route) {
            FontSizeScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.WebView.route,
            arguments = listOf(
                navArgument("articleId") { type = NavType.LongType },
                navArgument("feedId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getLong("articleId") ?: 0L
            val feedId = backStackEntry.arguments?.getLong("feedId") ?: 0L
            WebViewScreen(articleId = articleId, feedId = feedId, onBack = { navController.popBackStack() })
        }
    }
}
