package dev.panthu.sololife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.panthu.sololife.navigation.AppNavigation
import dev.panthu.sololife.navigation.Screen
import dev.panthu.sololife.ui.theme.SoloLifeTheme
import dev.panthu.sololife.util.hapticTick

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
        bottomBar = { AnimatedBottomNav(navController) },
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
    val icon: ImageVector
)

@Composable
private fun AnimatedBottomNav(navController: NavHostController) {
    val navItems = listOf(
        NavItem(Screen.Home,     "Home",     Icons.Rounded.Home),
        NavItem(Screen.Diary,    "Journal",  Icons.Rounded.AutoStories),
        NavItem(Screen.Expenses, "Expenses", Icons.Rounded.AccountBalanceWallet),
        NavItem(Screen.Settings, "Settings", Icons.Rounded.Tune)
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val topLevelRoutes = setOf(
        Screen.Home.route, Screen.Diary.route, Screen.Expenses.route, Screen.Settings.route
    )
    val showNav = currentDest?.route in topLevelRoutes

    val selectedIndex = navItems.indexOfFirst {
        currentDest?.hierarchy?.any { dest -> dest.route == it.screen.route } == true
    }.takeIf { it >= 0 } ?: 0

    val view = LocalView.current

    if (showNav) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
            ) {
                val itemWidth = maxWidth / navItems.size
                val pillWidth = 56.dp
                val pillHeight = 32.dp
                val pillVerticalOffset = (64.dp - pillHeight) / 2

                val pillOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex + (itemWidth - pillWidth) / 2,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "pill_offset"
                )

                // Sliding pill indicator
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset, y = pillVerticalOffset)
                        .size(width = pillWidth, height = pillHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    navItems.forEachIndexed { idx, item ->
                        val selected = idx == selectedIndex
                        val iconColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "icon_color_$idx"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    view.hapticTick()
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                AnimatedVisibility(visible = selected) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
