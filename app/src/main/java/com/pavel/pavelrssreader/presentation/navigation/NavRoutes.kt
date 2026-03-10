package com.pavel.pavelrssreader.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoutes(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
) {
    data object Articles : NavRoutes(
        route = "articles",
        label = "News",
        icon = Icons.Outlined.Newspaper,
        activeIcon = Icons.Filled.Newspaper
    )
    data object Favourites : NavRoutes(
        route = "favourites",
        label = "Favorites",
        icon = Icons.Outlined.StarBorder,
        activeIcon = Icons.Filled.Star
    )
    data object Feeds : NavRoutes(
        route = "feeds",
        label = "Feeds",
        icon = Icons.Outlined.RssFeed,
        activeIcon = Icons.Filled.RssFeed
    )
    data object Settings : NavRoutes(
        route = "settings",
        label = "Settings",
        icon = Icons.Outlined.Settings,
        activeIcon = Icons.Filled.Settings
    )
    data object WebView : NavRoutes(
        route = "webview/{articleId}",
        label = "Article",
        icon = Icons.Outlined.Newspaper,
        activeIcon = Icons.Filled.Newspaper
    ) {
        fun createRoute(articleId: Long) = "webview/$articleId"
    }
    data object FontSize : NavRoutes(
        route = "font_size",
        label = "Text Size",
        icon = Icons.Outlined.Settings,
        activeIcon = Icons.Filled.Settings
    )
}

val bottomNavItems = listOf(
    NavRoutes.Articles,
    NavRoutes.Favourites,
    NavRoutes.Feeds,
    NavRoutes.Settings
)
