package dev.panthu.sololife.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.data.db.DailyTotal
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.ui.components.AmountText
import dev.panthu.sololife.ui.components.AnimatedAmountText
import dev.panthu.sololife.ui.expenses.AddExpenseSheet
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.info
import dev.panthu.sololife.util.toExpenseCategory
import java.time.Instant
import java.time.ZoneId

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
        // ── Hero card pager ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -40 }
        ) {
            HeroCardPager(
                todayTotal = state.todayTotal,
                weekTotal = state.weekTotal,
                monthTotal = state.monthTotal,
                weekDailyTotals = state.weekDailyTotals,
                onAddExpense = { showExpenseSheet = true }
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Stat cards row ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) { 30 }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "This Week",
                    amount = state.weekTotal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    watermarkIcon = { Icon(Icons.Rounded.Insights, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)) },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "This Month",
                    amount = state.monthTotal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    watermarkIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Recent expenses ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) { 30 }
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
                            .padding(horizontal = 24.dp)
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
                            .padding(horizontal = 24.dp)
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

        Spacer(Modifier.height(28.dp))

        // ── Latest diary entry ──────────────────────────────────────────
        val latest = state.latestDiaryEntry
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy)) { 30 }
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
                            .padding(horizontal = 24.dp)
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

// ── Hero card pager ───────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HeroCardPager(
    todayTotal: Double,
    weekTotal: Double,
    monthTotal: Double,
    weekDailyTotals: List<DailyTotal>,
    onAddExpense: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val heroShape = RoundedCornerShape(28.dp)
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Surface(
            shape = heroShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to primaryColor.copy(alpha = 0.18f),
                                0.5f to tertiaryColor.copy(alpha = 0.10f),
                                1f to Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 0.5.dp,
                        color = outlineColor.copy(alpha = 0.15f),
                        shape = heroShape
                    )
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> HeroPageToday(
                            todayTotal = todayTotal,
                            onAddExpense = onAddExpense
                        )
                        1 -> HeroPageWeek(
                            weekTotal = weekTotal,
                            dailyTotals = weekDailyTotals
                        )
                        2 -> HeroPageMonth(monthTotal = monthTotal)
                        else -> Unit
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Dot indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val isSelected = pagerState.currentPage == index
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 20.dp else 6.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dotWidth$index"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(dotWidth)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) primaryColor
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
                        )
                )
            }
        }
    }
}

// ── Hero page: Today ──────────────────────────────────────────────────────────

@Composable
private fun HeroPageToday(todayTotal: Double, onAddExpense: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = DateUtils.greetingPrefix(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = DateUtils.formatFull(System.currentTimeMillis()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(20.dp))
        AnimatedAmountText(
            amount = todayTotal,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAddExpense,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Add Expense", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Hero page: Week ───────────────────────────────────────────────────────────

@Composable
private fun HeroPageWeek(weekTotal: Double, dailyTotals: List<DailyTotal>) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "This Week",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        AnimatedAmountText(
            amount = weekTotal,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        WeekSparkline(dailyTotals = dailyTotals)
    }
}

// ── Hero page: Month ──────────────────────────────────────────────────────────

@Composable
private fun HeroPageMonth(monthTotal: Double) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "This Month",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        AnimatedAmountText(
            amount = monthTotal,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(28.dp))
    }
}

// ── Week sparkline ────────────────────────────────────────────────────────────

@Composable
private fun WeekSparkline(dailyTotals: List<DailyTotal>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val todayStartMs = DateUtils.todayStart()
    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    // Resolve today index from the daily totals list (Mon=0 … Sun=6)
    val todayIndex = dailyTotals.indexOfFirst { it.dayStart == todayStartMs }

    // Fallback: compute today's weekday index (Mon=0) from system time
    val resolvedTodayIndex = if (todayIndex >= 0) todayIndex else run {
        val dow = Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .dayOfWeek.value - 1 // ISO: Mon=1..Sun=7 => 0..6
        dow
    }

    val maxTotal = dailyTotals.maxOfOrNull { it.total } ?: 0.0

    Canvas(
        modifier = Modifier
            .width(200.dp)
            .height(80.dp)
    ) {
        val count = 7
        val labelAreaHeight = 18.dp.toPx()
        val barAreaHeight = size.height - labelAreaHeight
        val totalWidth = size.width
        val barWidth = totalWidth / (count * 2f - 1f)
        val gap = barWidth
        val cornerRad = CornerRadius(barWidth / 2f, barWidth / 2f)
        val minBarHeight = 6.dp.toPx()

        dailyTotals.forEachIndexed { i, daily ->
            val x = i * (barWidth + gap)
            val isToday = i == resolvedTodayIndex
            val hasSpend = daily.total > 0.0

            val barHeight = if (maxTotal <= 0.0 || !hasSpend) {
                minBarHeight
            } else {
                (daily.total / maxTotal * barAreaHeight).toFloat().coerceAtLeast(minBarHeight)
            }
            val barTop = barAreaHeight - barHeight

            if (!hasSpend) {
                // Zero-spend: outline (stroke) bar only
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.20f),
                    topLeft = Offset(x, barTop),
                    size = Size(barWidth, barHeight + cornerRad.y),
                    cornerRadius = cornerRad,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                // Filled bar; extend height below canvas bottom so bottom corners are clipped
                drawRoundRect(
                    color = if (isToday) primaryColor else primaryColor.copy(alpha = 0.40f),
                    topLeft = Offset(x, barTop),
                    size = Size(barWidth, barHeight + cornerRad.y),
                    cornerRadius = cornerRad
                )
            }
        }
    }

    // Day labels row
    Row(
        modifier = Modifier.width(200.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dayLabels.forEachIndexed { i, label ->
            val isToday = i == resolvedTodayIndex
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    amount: Double,
    containerColor: androidx.compose.ui.graphics.Color,
    watermarkIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                watermarkIcon()
            }
        }
    }
}

// ── Section headers ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeaderWithAction(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
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
            .padding(horizontal = 24.dp),
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

// ── Recent expense row ────────────────────────────────────────────────────────

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

// ── Diary preview card ────────────────────────────────────────────────────────

@Composable
private fun DiaryPreviewCard(
    entry: dev.panthu.sololife.data.db.DiaryEntry,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
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
