package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoutes(val route: String, val label: String, val icon: ImageVector) {
    data object Articles : NavRoutes("articles", "Feed", Icons.Default.Home)
    data object Favourites : NavRoutes("favourites", "Favourites", Icons.Default.Favorite)
    data object Feeds : NavRoutes("feeds", "Feeds", Icons.Default.Menu)
    data object WebView : NavRoutes("webview/{articleId}", "Article", Icons.Default.Home) {
        fun createRoute(articleId: Long) = "webview/$articleId"
    }
    data object Settings : NavRoutes("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(NavRoutes.Articles, NavRoutes.Favourites, NavRoutes.Feeds, NavRoutes.Settings)
