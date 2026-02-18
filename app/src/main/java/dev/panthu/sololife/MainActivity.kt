package dev.panthu.sololife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.panthu.sololife.navigation.AppNavigation
import dev.panthu.sololife.navigation.Screen
import dev.panthu.sololife.ui.theme.SoloLifeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoloLifeTheme {
                MainContent()
            }
        }
    }
}

@Composable
private fun MainContent() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNav(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(navController = navController)
        }
    }
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
private fun BottomNav(navController: NavHostController) {
    val navItems = listOf(
        NavItem(Screen.Home,     "Home",     { Icon(Icons.Rounded.Home,        "Home") }),
        NavItem(Screen.Diary,    "Diary",    { Icon(Icons.AutoMirrored.Rounded.MenuBook,    "Diary") }),
        NavItem(Screen.Expenses, "Expenses", { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, "Expenses") }),
        NavItem(Screen.Settings, "Settings", { Icon(Icons.Rounded.Settings,    "Settings") })
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    // Hide bottom nav on sub-screens (detail, settings)
    val topLevelRoutes = setOf(Screen.Home.route, Screen.Diary.route, Screen.Expenses.route, Screen.Settings.route)
    val showNav = currentDest?.route in topLevelRoutes

    if (showNav) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            navItems.forEach { item ->
                val selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = item.icon,
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}
