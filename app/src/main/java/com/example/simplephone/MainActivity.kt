package com.example.simplephone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.simplephone.ui.components.AppScaffold
import com.example.simplephone.ui.components.Screen
import com.example.simplephone.ui.screens.FavoritesScreen
import com.example.simplephone.ui.screens.MainScreen
import com.example.simplephone.ui.screens.PhoneBookScreen
import com.example.simplephone.ui.screens.RecentsScreen
import com.example.simplephone.ui.screens.SettingsScreen
import com.example.simplephone.ui.theme.SimplePhoneTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimplePhoneTheme {
                val windowSize = calculateWindowSizeClass(this)
                SimplePhoneApp(windowSize.widthSizeClass)
            }
        }
    }
}

@Composable
fun SimplePhoneApp(widthSizeClass: WindowWidthSizeClass) {
    val navController = rememberNavController()
    var filterHours by remember { mutableStateOf(2) } // State lifted up

    // Determine current screen title for TopBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTitle = when(currentRoute) {
        Screen.Settings.route -> Screen.Settings.title
        else -> "Simple Phone"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppScaffold(navController = navController, currentScreenTitle = currentTitle) {
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    MainScreen(
                        onCallClick = { /* Simulate Call */ },
                        onContactClick = { /* Simulate Contact View */ }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        filterHours = filterHours,
                        onFilterChange = { filterHours = it }
                    )
                }
            }
        }
    }
}
