package com.mozilla.buildwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mozilla.buildwatch.ui.screen.PushDetailScreen
import com.mozilla.buildwatch.ui.screen.SettingsScreen
import com.mozilla.buildwatch.ui.screen.TryPushesScreen
import com.mozilla.buildwatch.ui.theme.BuildWatchTheme
import kotlinx.serialization.Serializable

// Type-safe nav routes
@Serializable object TryPushes
@Serializable object Settings
@Serializable data class PushDetail(val pushId: Int)

private data class TabItem(val route: Any, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(TryPushes, "Try Pushes", Icons.Filled.List),
    TabItem(Settings, "Settings", Icons.Filled.Settings),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuildWatchTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentDest = backStack?.destination

                val showBottomBar = currentDest?.let { dest ->
                    tabs.any { dest.hasRoute(it.route::class) }
                } ?: true

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                tabs.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentDest?.hasRoute(tab.route::class) == true,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) },
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    NavHost(navController = navController, startDestination = TryPushes) {
                        composable<TryPushes> {
                            TryPushesScreen(onPushClick = { pushId ->
                                navController.navigate(PushDetail(pushId))
                            })
                        }
                        composable<Settings> {
                            SettingsScreen()
                        }
                        composable<PushDetail> { entry ->
                            val route = entry.toRoute<PushDetail>()
                            PushDetailScreen(pushId = route.pushId)
                        }
                    }
                }
            }
        }
    }
}
