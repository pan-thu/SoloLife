package dev.panthu.sololife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
        bottomBar = { FloatingIslandNav(navController) },
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
private fun FloatingIslandNav(navController: NavHostController) {
    val navItems = listOf(
        NavItem(Screen.Home,     "Home",     Icons.Rounded.Home),
        NavItem(Screen.Diary,    "Journal",  Icons.Rounded.AutoStories),
        NavItem(Screen.Expenses, "Wallet",   Icons.Rounded.AccountBalanceWallet),
        NavItem(Screen.Settings, "Settings", Icons.Rounded.Tune)
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val topLevelRoutes = setOf(
        Screen.Home.route, Screen.Diary.route, Screen.Expenses.route, Screen.Settings.route
    )
    val showNav = currentDest?.route in topLevelRoutes

    val selectedIndex = navItems.indexOfFirst {
        currentDest?.hierarchy?.any { d -> d.route == it.screen.route } == true
    }.takeIf { it >= 0 } ?: 0

    val view = LocalView.current
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline

    AnimatedVisibility(
        visible = showNav,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it }
    ) {
        // Floating island container — full width wrapper for nav bar padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val totalWidth = maxWidth
                val itemWidth = totalWidth / navItems.size
                val indicatorW = 44.dp

                // Animated indicator x offset
                val indicatorX by animateDpAsState(
                    targetValue = itemWidth * selectedIndex + (itemWidth - indicatorW) / 2,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
                    label = "indicatorX"
                )

                // ── Nav island surface ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .drawBehind {
                            // Glow shadow under the bar — colored by primary
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                                    center = Offset(
                                        size.width * (selectedIndex + 0.5f) / navItems.size,
                                        size.height * 0.7f
                                    ),
                                    radius = size.width * 0.35f
                                ),
                                radius = size.width * 0.35f,
                                center = Offset(
                                    size.width * (selectedIndex + 0.5f) / navItems.size,
                                    size.height * 0.7f
                                )
                            )
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(surface)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    primary.copy(alpha = 0.28f),
                                    outline.copy(alpha = 0.08f),
                                    outline.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    // Sliding luminous indicator pill
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorX, y = 9.dp)
                            .size(width = indicatorW, height = 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        primary.copy(alpha = 0.18f),
                                        primary.copy(alpha = 0.08f)
                                    )
                                )
                            )
                    )

                    // Icons row
                    Row(modifier = Modifier.fillMaxSize()) {
                        navItems.forEachIndexed { idx, item ->
                            val selected = idx == selectedIndex

                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.12f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "iconScale_$idx"
                            )
                            val iconColor by animateColorAsState(
                                targetValue = if (selected) primary
                                              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                animationSpec = tween(200),
                                label = "iconColor_$idx"
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
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = iconColor,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .scale(iconScale)
                                    )

                                    // Glowing dot for selected tab
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(if (selected) 4.dp else 0.dp)
                                            .clip(CircleShape)
                                            .background(primary)
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
