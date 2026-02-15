package dev.panthu.sololife.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.panthu.sololife.data.db.ExpenseCategory
import dev.panthu.sololife.ui.theme.CategoryEntertainment
import dev.panthu.sololife.ui.theme.CategoryFood
import dev.panthu.sololife.ui.theme.CategoryGroceries
import dev.panthu.sololife.ui.theme.CategoryHealth
import dev.panthu.sololife.ui.theme.CategoryOther
import dev.panthu.sololife.ui.theme.CategoryTransport
import dev.panthu.sololife.ui.theme.CategoryUtilities

data class CategoryInfo(val label: String, val color: Color, val icon: ImageVector)

fun ExpenseCategory.info(): CategoryInfo = when (this) {
    ExpenseCategory.FOOD          -> CategoryInfo("Food",          CategoryFood,          Icons.Rounded.Restaurant)
    ExpenseCategory.GROCERIES     -> CategoryInfo("Groceries",     CategoryGroceries,     Icons.Rounded.LocalGroceryStore)
    ExpenseCategory.TRANSPORT     -> CategoryInfo("Transport",     CategoryTransport,     Icons.Rounded.DirectionsCar)
    ExpenseCategory.ENTERTAINMENT -> CategoryInfo("Entertainment", CategoryEntertainment, Icons.Rounded.Theaters)
    ExpenseCategory.HEALTH        -> CategoryInfo("Health",        CategoryHealth,        Icons.Rounded.FavoriteBorder)
    ExpenseCategory.UTILITIES     -> CategoryInfo("Utilities",     CategoryUtilities,     Icons.Rounded.Bolt)
    ExpenseCategory.OTHER         -> CategoryInfo("Other",         CategoryOther,         Icons.Rounded.GridView)
}

fun String.toExpenseCategory(): ExpenseCategory =
    ExpenseCategory.entries.firstOrNull { it.name == this } ?: ExpenseCategory.OTHER
