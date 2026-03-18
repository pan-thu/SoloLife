package dev.panthu.sololife.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.ui.components.AmbientOrbs
import dev.panthu.sololife.ui.expenses.ExpenseFormSheet
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.info
import dev.panthu.sololife.util.toExpenseCategory
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun HomeScreen(
    onNavigateDiary: () -> Unit,
    onNavigateExpenses: () -> Unit,
    onNewDiaryEntry: () -> Unit,
    onOpenDiaryEntry: (Long) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    var showExpenseSheet by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val bg = MaterialTheme.colorScheme.background
    val pagerState = rememberPagerState(pageCount = { 2 })

    Box(modifier = Modifier.fillMaxSize().background(bg)) {

        AmbientOrbs(primary = primary, tertiary = tertiary, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            GreetingHeader(visible = visible, pagerState = pagerState, primary = primary)

            Spacer(Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> ExpensesPage(
                        state = state,
                        visible = visible,
                        primary = primary,
                        tertiary = tertiary,
                        onNavigateExpenses = onNavigateExpenses,
                        onAddExpense = { showExpenseSheet = true }
                    )
                    else -> DiaryPage(
                        state = state,
                        visible = visible,
                        primary = primary,
                        tertiary = tertiary,
                        onNewDiaryEntry = onNewDiaryEntry,
                        onOpenDiaryEntry = onOpenDiaryEntry,
                        onNavigateDiary = onNavigateDiary
                    )
                }
            }
        }
    }

    if (showExpenseSheet) {
        ExpenseFormSheet(
            expense = null,
            onDismiss = { showExpenseSheet = false },
            onSave = { amount, category, description, date ->
                vm.addExpense(amount, category, description, date)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expenses page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpensesPage(
    state: HomeUiState,
    visible: Boolean,
    primary: Color,
    tertiary: Color,
    onNavigateExpenses: () -> Unit,
    onAddExpense: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 80)) +
                    slideInVertically(tween(500, delayMillis = 80)) { 60 }
        ) {
            WaterTankCard(
                todayTotal = state.todayTotal,
                weekTotal = state.weekTotal,
                primary = primary,
                onAddExpense = onAddExpense
            )
        }

        Spacer(Modifier.height(14.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 160)) +
                    slideInVertically(tween(500, delayMillis = 160)) { 50 }
        ) {
            BentoMetricRow(
                weekTotal = state.weekTotal,
                lastWeekTotal = state.lastWeekTotal,
                monthTotal = state.monthTotal,
                lastMonthTotal = state.lastMonthTotal,
                primary = primary,
                tertiary = tertiary
            )
        }

        Spacer(Modifier.height(28.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 240)) +
                    slideInVertically(tween(500, delayMillis = 240)) { 50 }
        ) {
            RecentExpensesSection(
                expenses = state.recentExpenses,
                onSeeAll = onNavigateExpenses,
                primary = primary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diary page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiaryPage(
    state: HomeUiState,
    visible: Boolean,
    primary: Color,
    tertiary: Color,
    onNewDiaryEntry: () -> Unit,
    onOpenDiaryEntry: (Long) -> Unit,
    onNavigateDiary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 80)) +
                    slideInVertically(tween(500, delayMillis = 80)) { 60 }
        ) {
            StreakHeroCard(
                streak = state.diaryStreak,
                weekDays = state.diaryWeekDays,
                primary = primary,
                onNewEntry = onNewDiaryEntry
            )
        }

        Spacer(Modifier.height(28.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 180)) +
                    slideInVertically(tween(500, delayMillis = 180)) { 50 }
        ) {
            DiarySnippetSection(
                entry = state.latestDiaryEntry,
                onNewEntry = onNewDiaryEntry,
                onOpenEntry = { state.latestDiaryEntry?.let { onOpenDiaryEntry(it.id) } },
                onSeeAll = onNavigateDiary,
                primary = primary,
                tertiary = tertiary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Streak hero card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StreakHeroCard(
    streak: Int,
    weekDays: List<Boolean>,
    primary: Color,
    onNewEntry: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val animStreak by animateIntAsState(
        targetValue = streak,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "streak"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(primary.copy(0.35f), outline.copy(0.08f))),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 24.dp, vertical = 26.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                "WRITING STREAK",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "$animStreak",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp
                ),
                color = if (streak > 0) primary else onSurfaceVariant
            )
            Text(
                if (streak == 1) "day in a row" else "days in a row",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant
            )
            if (streak == 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Write your first entry today!",
                    style = MaterialTheme.typography.labelSmall,
                    color = primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(22.dp))

            WeekDotsRow(weekDays = weekDays, primary = primary)

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onNewEntry,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Entry", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WeekDotsRow(weekDays: List<Boolean>, primary: Color) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayLabels.forEachIndexed { i, label ->
            val hasEntry = weekDays.getOrElse(i) { false }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (hasEntry) primary else primary.copy(0.08f))
                        .border(1.dp, if (hasEntry) Color.Transparent else primary.copy(0.20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasEntry) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasEntry) primary else onSurfaceVariant,
                    fontWeight = if (hasEntry) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Greeting header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GreetingHeader(visible: Boolean, pagerState: PagerState, primary: Color) {
    val scope = rememberCoroutineScope()
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surface
    val labels = listOf("Expenses", "Diary")

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -24 }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(primary)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = DateUtils.greetingPrefix().uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = DateUtils.formatFull(System.currentTimeMillis()),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Tab pills replacing the pulse dot
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEachIndexed { index, label ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) primary else surface)
                            .border(
                                1.dp,
                                if (selected) Color.Transparent else primary.copy(0.22f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { scope.launch { pagerState.animateScrollToPage(index) } }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) onPrimary else primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Water tank card — hero widget
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WaterTankCard(
    todayTotal: Double,
    weekTotal: Double,
    primary: Color,
    onAddExpense: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val progress by animateFloatAsState(
        targetValue = if (weekTotal <= 0.0) 0.05f
                      else (todayTotal / weekTotal).toFloat().coerceIn(0.05f, 1f),
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "tankProgress"
    )

    // The card IS the tank — clip rounds it, water fills from the bottom
    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(surface),
        contentAlignment = Alignment.Center
    ) {
        // Water animation spans the entire card
        WaterTankCanvas(progress = progress, primary = primary)

        // Centered content overlaid on the water
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            val onSurface = MaterialTheme.colorScheme.onSurface
            val animated by animateFloatAsState(
                targetValue = todayTotal.toFloat(),
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                label = "todayAnim"
            )
            Text(
                "TODAY",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = onSurface.copy(alpha = 0.70f),
                letterSpacing = 2.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "฿${"%.2f".format(animated)}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp
                ),
                color = onSurface
            )
            if (weekTotal > 0.0) {
                val pct = ((todayTotal / weekTotal) * 100).toInt().coerceAtMost(100)
                Spacer(Modifier.height(1.dp))
                Text(
                    "$pct% of this week",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = onSurface.copy(alpha = 0.70f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Button — surface-colored so it floats above water
            Button(
                onClick = onAddExpense,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = surface,
                    contentColor = primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Add Expense",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WaterTankCanvas(progress: Float, primary: Color) {
    val inf = rememberInfiniteTransition(label = "water")

    val wave1Phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "wave1"
    )
    val wave2Phase by inf.animateFloat(
        initialValue = (2.0 * Math.PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave2"
    )

    // Parent Box already clips to card shape — canvas just draws the water
    Canvas(modifier = Modifier.fillMaxSize()) {
        val waterLevel = size.height - size.height * progress
        val freq = (2.0 * Math.PI / size.width).toFloat()

        // ── Solid water body (below waves) ───────────────────────────────
        drawRect(
            brush = Brush.verticalGradient(
                listOf(primary.copy(0.14f), primary.copy(0.26f)),
                startY = waterLevel + 14.dp.toPx(),
                endY = size.height
            ),
            topLeft = Offset(0f, waterLevel + 14.dp.toPx()),
            size = Size(size.width, size.height - waterLevel)
        )

        // ── Background wave (slower, lighter) ────────────────────────────
        val bgWavePath = Path()
        val bgAmp = 6.dp.toPx()
        var x = 0f; var first = true
        while (x <= size.width + 1f) {
            val y = waterLevel + bgAmp * sin((freq * x * 0.85f + wave2Phase).toDouble()).toFloat() + 5.dp.toPx()
            if (first) { bgWavePath.moveTo(x, y); first = false } else bgWavePath.lineTo(x, y)
            x += 2f
        }
        bgWavePath.lineTo(size.width, size.height)
        bgWavePath.lineTo(0f, size.height)
        bgWavePath.close()
        drawPath(bgWavePath, Brush.verticalGradient(
            listOf(primary.copy(0.11f), primary.copy(0.20f)),
            startY = waterLevel, endY = size.height
        ))

        // ── Foreground wave (faster, more opaque) ────────────────────────
        val fgWavePath = Path()
        val fgAmp = 8.dp.toPx()
        var x2 = 0f; var first2 = true
        while (x2 <= size.width + 1f) {
            val y = waterLevel + fgAmp * sin((freq * x2 + wave1Phase).toDouble()).toFloat()
            if (first2) { fgWavePath.moveTo(x2, y); first2 = false } else fgWavePath.lineTo(x2, y)
            x2 += 2f
        }
        fgWavePath.lineTo(size.width, size.height)
        fgWavePath.lineTo(0f, size.height)
        fgWavePath.close()
        drawPath(fgWavePath, Brush.verticalGradient(
            listOf(primary.copy(0.18f), primary.copy(0.30f)),
            startY = waterLevel - fgAmp, endY = size.height
        ))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bento metric row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BentoMetricRow(
    weekTotal: Double,
    lastWeekTotal: Double,
    monthTotal: Double,
    lastMonthTotal: Double,
    primary: Color,
    tertiary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricBentoCard(
            label = "THIS WEEK",
            amount = weekTotal,
            lastAmount = lastWeekTotal,
            periodLabel = "week",
            accentColor = primary,
            modifier = Modifier.weight(1f)
        )
        MetricBentoCard(
            label = "THIS MONTH",
            amount = monthTotal,
            lastAmount = lastMonthTotal,
            periodLabel = "month",
            accentColor = tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricBentoCard(
    label: String,
    amount: Double,
    lastAmount: Double,
    periodLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val hasPrev = lastAmount > 0.0
    val diff = amount - lastAmount
    val pct = if (hasPrev) ((diff / lastAmount) * 100).toInt() else 0
    val increased = diff > 0.0
    val changeColor = if (increased) Color(0xFFE05C5C) else Color(0xFF52B788)

    val animAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "metricAnim"
    )
    val animLast by animateFloatAsState(
        targetValue = lastAmount.toFloat(),
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "lastAnim"
    )

    Box(
        modifier = modifier
            .heightIn(min = 130.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                brush = Brush.linearGradient(listOf(accentColor.copy(0.22f), outline.copy(0.07f))),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 22.dp)) {

            // Label row + change badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                if (hasPrev) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(changeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${if (increased) "↑" else "↓"} ${kotlin.math.abs(pct)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = changeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Current amount
            Text(
                "฿${"%.2f".format(animAmount)}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = accentColor
            )
        }

        // Last period — bottom right, above the accent line
        Text(
            if (hasPrev) "฿${"%.2f".format(animLast)} last $periodLabel" else "— last $periodLabel",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = onSurfaceVariant.copy(alpha = if (hasPrev) 0.90f else 0.50f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 10.dp)
        )

        // Full-width accent line at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(accentColor.copy(0.35f), accentColor)))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent expenses — horizontal scroll cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentExpensesSection(
    expenses: List<Expense>,
    onSeeAll: () -> Unit,
    primary: Color
) {
    Column {
        SectionHeader(title = "Recent Expenses", actionLabel = "See All", onAction = onSeeAll, primary = primary)
        Spacer(Modifier.height(12.dp))

        if (expenses.isEmpty()) {
            EmptySlot(message = "No expenses yet", modifier = Modifier.padding(horizontal = 22.dp))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseChip(expense = expense)
                }
            }
        }
    }
}

@Composable
private fun ExpenseChip(expense: Expense) {
    val cat = expense.category.toExpenseCategory()
    val info = cat.info()
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Brush.linearGradient(listOf(info.color.copy(0.3f), outline.copy(0.06f))), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(info.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(info.icon, contentDescription = null, tint = info.color, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (expense.description.isBlank()) info.label else expense.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "฿${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = info.color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                DateUtils.formatShort(expense.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diary snippet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiarySnippetSection(
    entry: DiaryEntry?,
    onNewEntry: () -> Unit,
    onOpenEntry: () -> Unit,
    onSeeAll: () -> Unit,
    primary: Color,
    tertiary: Color
) {
    Column {
        // Header with dual actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Latest Entry",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(primary.copy(alpha = 0.10f))
                        .clickable(onClick = onNewEntry)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(11.dp), tint = primary)
                        Text("New", style = MaterialTheme.typography.labelSmall, color = primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onSeeAll, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("All", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (entry != null) {
            NotebookCard(entry = entry, onClick = onOpenEntry, primary = primary, tertiary = tertiary)
        } else {
            EmptySlot(message = "Start writing your story", modifier = Modifier.padding(horizontal = 22.dp))
        }
    }
}

@Composable
private fun NotebookCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    primary: Color,
    tertiary: Color
) {
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)

    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(primary.copy(0.18f), tertiary.copy(0.10f))),
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Notebook rule lines
        Canvas(modifier = Modifier.fillMaxWidth().height(118.dp)) {
            val lineH = 22.dp.toPx()
            val leftM = 60.dp.toPx()
            var y = 28.dp.toPx()
            while (y < size.height) {
                drawLine(lineColor, Offset(leftM, y), Offset(size.width - 20.dp.toPx(), y), 1.dp.toPx())
                y += lineH
            }
            // Margin rule
            drawLine(tertiary.copy(0.22f), Offset(52.dp.toPx(), 0f), Offset(52.dp.toPx(), size.height), 1.2.dp.toPx())
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Date badge
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
                Text(
                    DateUtils.formatDay(entry.date),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
                    color = primary
                )
                Text(
                    DateUtils.formatShort(entry.date).take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.content.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        entry.content.replace(Regex("<[^>]*>"), "").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.45f),
                modifier = Modifier.size(18.dp).align(Alignment.CenterVertically)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared small components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, actionLabel: String = "", onAction: () -> Unit = {}, primary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel.isNotEmpty()) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = primary)
                Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(14.dp), tint = primary)
            }
        }
    }
}

@Composable
private fun EmptySlot(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
