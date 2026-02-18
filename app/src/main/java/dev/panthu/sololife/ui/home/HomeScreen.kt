package dev.panthu.sololife.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.ui.components.AmountText
import dev.panthu.sololife.ui.expenses.AddExpenseSheet
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.info
import dev.panthu.sololife.util.toExpenseCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateDiary: () -> Unit,
    onNavigateExpenses: () -> Unit,
    onNewDiaryEntry: () -> Unit,
    onOpenDiaryEntry: (Long) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showExpenseSheet by remember { mutableStateOf(false) }

    // Staggered entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // ── Hero card ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }
        ) {
            HeroCard(
                todayTotal = state.todayTotal,
                onAddExpense = { showExpenseSheet = true }
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Stat cards row ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 100)) +
                    slideInVertically(tween(500, delayMillis = 100)) { 30 }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "This Week",
                    amount = state.weekTotal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "This Month",
                    amount = state.monthTotal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Recent expenses ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 200)) +
                    slideInVertically(tween(500, delayMillis = 200)) { 30 }
        ) {
            Column {
                SectionHeaderWithAction(
                    title = "Recent Expenses",
                    actionLabel = "See All",
                    onAction = onNavigateExpenses
                )
                Spacer(Modifier.height(8.dp))
                if (state.recentExpenses.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No expenses yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            state.recentExpenses.forEachIndexed { index, expense ->
                                RecentExpenseRow(expense = expense)
                                if (index < state.recentExpenses.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Latest diary entry ──────────────────────────────────────────
        val latest = state.latestDiaryEntry
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 300)) +
                    slideInVertically(tween(500, delayMillis = 300)) { 30 }
        ) {
            Column {
                SectionHeaderWithTwoActions(
                    title = "Latest Entry",
                    primaryActionLabel = "New",
                    onPrimaryAction = onNewDiaryEntry,
                    secondaryActionLabel = "All",
                    onSecondaryAction = onNavigateDiary
                )
                Spacer(Modifier.height(8.dp))
                if (latest != null) {
                    DiaryPreviewCard(
                        entry = latest,
                        onClick = { onOpenDiaryEntry(latest.id) }
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No diary entries yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Expense sheet — hosted on top of HomeScreen, no navigation needed
    if (showExpenseSheet) {
        AddExpenseSheet(
            onDismiss = { showExpenseSheet = false },
            onAdd = { amount, category, description, date ->
                vm.addExpense(amount, category, description, date)
            }
        )
    }
}

// ── Hero card ────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(todayTotal: Double, onAddExpense: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryContainer.copy(alpha = 0.6f),
                            primaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = DateUtils.formatDayName(System.currentTimeMillis()),
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateUtils.formatFull(System.currentTimeMillis()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "Today's Spending",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        AmountText(
                            amount = todayTotal,
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                    FilledTonalButton(
                        onClick = onAddExpense,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = primaryColor.copy(alpha = 0.15f),
                            contentColor = primaryColor
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ── Stat card ────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    amount: Double,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            AmountText(
                amount = amount,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

// ── Section headers ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeaderWithAction(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionHeaderWithTwoActions(
    title: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row {
            // "New" button — takes you directly to the compose screen
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onPrimaryAction),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "New entry",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        primaryActionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            // "All" text button
            TextButton(
                onClick = onSecondaryAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    secondaryActionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Recent expense row ───────────────────────────────────────────────────────

@Composable
private fun RecentExpenseRow(expense: Expense) {
    val category = expense.category.toExpenseCategory()
    val catInfo = category.info()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(catInfo.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = catInfo.icon,
                contentDescription = null,
                tint = catInfo.color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (expense.description.isBlank()) catInfo.label else expense.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = if (expense.description.isNotBlank()) catInfo.label
                       else DateUtils.formatShort(expense.date),
                style = MaterialTheme.typography.labelSmall,
                color = catInfo.color
            )
        }
        AmountText(
            amount = expense.amount,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

// ── Diary preview card ───────────────────────────────────────────────────────

@Composable
private fun DiaryPreviewCard(
    entry: dev.panthu.sololife.data.db.DiaryEntry,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = DateUtils.formatDay(entry.date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = entry.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = DateUtils.formatFull(entry.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (entry.content.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }
        }
    }
}
