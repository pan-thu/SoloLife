package dev.panthu.sololife.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.panthu.sololife.ui.diary.DiaryDetailScreen
import dev.panthu.sololife.ui.diary.DiaryListScreen
import dev.panthu.sololife.ui.expenses.ExpensesScreen
import dev.panthu.sololife.ui.home.HomeScreen
import dev.panthu.sololife.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home     : Screen("home")
    data object Diary    : Screen("diary")
    data object Expenses : Screen("expenses")
    data object Settings : Screen("settings")
    data object DiaryNew : Screen("diary/new")
    data object DiaryDetail : Screen("diary/{entryId}") {
        fun route(id: Long) = "diary/$id"
    }
}

// Top-level tabs get a subtle crossfade; sub-screens slide in from right
private val tabEnter = fadeIn(tween(250))
private val tabExit  = fadeOut(tween(200))

private val subEnter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }
private val subExit  = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }
private val subPopEnter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
private val subPopExit  = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 }

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { tabEnter },
        exitTransition = { tabExit },
        popEnterTransition = { tabEnter },
        popExitTransition = { tabExit }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateDiary    = { navController.navigate(Screen.Diary.route) },
                onNavigateExpenses = { navController.navigate(Screen.Expenses.route) },
                onNewDiaryEntry    = { navController.navigate(Screen.DiaryNew.route) },
                onOpenDiaryEntry   = { id -> navController.navigate(Screen.DiaryDetail.route(id)) }
            )
        }
        composable(Screen.Diary.route) {
            DiaryListScreen(
                onOpenEntry = { id -> navController.navigate(Screen.DiaryDetail.route(id)) },
                onNewEntry  = { navController.navigate(Screen.DiaryNew.route) }
            )
        }
        composable(
            route = Screen.DiaryNew.route,
            enterTransition = { subEnter },
            exitTransition = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition = { subPopExit }
        ) {
            DiaryDetailScreen(entryId = null, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.DiaryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            enterTransition = { subEnter },
            exitTransition = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition = { subPopExit }
        ) { backStack ->
            val id = backStack.arguments?.getLong("entryId")
            DiaryDetailScreen(entryId = id, onBack = { navController.popBackStack() })
        }
        composable(Screen.Expenses.route) {
            ExpensesScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
