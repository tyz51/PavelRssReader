package com.pavel.pavelrssreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pavel.pavelrssreader.domain.model.ThemePreference
import com.pavel.pavelrssreader.presentation.navigation.BottomNavBar
import com.pavel.pavelrssreader.presentation.navigation.NavGraph
import com.pavel.pavelrssreader.presentation.navigation.NavRoutes
import com.pavel.pavelrssreader.presentation.settings.SettingsViewModel
import com.pavel.pavelrssreader.ui.theme.PavelRssReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by settingsViewModel.uiState.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (state.themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemDark
            }

            PavelRssReaderTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                // Hide bottom nav on webview and font size screens
                val showBottomBar = currentRoute != null &&
                    !currentRoute.startsWith("webview/") &&
                    currentRoute != NavRoutes.FontSize.route

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) BottomNavBar(navController)
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
