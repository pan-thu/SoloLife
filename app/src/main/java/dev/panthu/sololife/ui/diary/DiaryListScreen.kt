package dev.panthu.sololife.ui.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells.Fixed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.ui.components.AmbientOrbs
import dev.panthu.sololife.ui.components.DiaryEmptyState
import dev.panthu.sololife.ui.components.ShimmerDiaryCard
import dev.panthu.sololife.ui.components.SwipeActionsContainer
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.hapticConfirm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    onOpenEntry: (Long) -> Unit,
    onNewEntry: () -> Unit,
    vm: DiaryViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val view = LocalView.current

    var calendarExpanded by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    AmbientOrbs(primary = primary, tertiary = tertiary, modifier = Modifier.fillMaxSize())
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Diary", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    // Streak badge
                    if (state.currentStreak > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    state.currentStreak.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    // Calendar toggle
                    IconButton(onClick = {
                        calendarExpanded = !calendarExpanded
                        if (!calendarExpanded) pendingDate = null
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Toggle calendar"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { view.hapticConfirm(); onNewEntry() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = "New entry")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Date filter chip
                val selectedDate = state.selectedDate
                if (selectedDate != null) {
                    val label = selectedDate.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    )
                    FilterChip(
                        selected = true,
                        onClick = { vm.clearDateFilter() },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                // Collapsible calendar panel
                AnimatedVisibility(visible = calendarExpanded) {
                    val zone = ZoneId.systemDefault()
                    val monthStart = LocalDate.now(zone).withDayOfMonth(1)
                        .atStartOfDay(zone).toInstant().toEpochMilli()
                    DiaryCalendarView(
                        currentMonthStart = monthStart,
                        entryDates = state.calendarEntryDates,
                        selectedDate = state.selectedDate,
                        pendingDate = pendingDate,
                        onDayClick = { date -> pendingDate = if (pendingDate == date) null else date },
                        onConfirm = {
                            pendingDate?.let { vm.setSelectedDate(it) }
                            pendingDate = null
                            calendarExpanded = false
                        }
                    )
                }

                // Entry list — always visible
                when {
                    !state.isLoaded -> {
                        LazyColumn {
                            items(5) { ShimmerDiaryCard() }
                        }
                    }
                    state.entries.isEmpty() -> {
                        DiaryEmptyState()
                    }
                    else -> {
                        val grouped = remember(state.entries) { state.entries.groupByMonth() }
                        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                            grouped.forEach { (monthKey, entries) ->
                                item(key = "header_$monthKey") {
                                    PillMonthHeader(millis = monthKey)
                                }
                                itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                                    SwipeActionsContainer(
                                        item = entry,
                                        onDelete = { vm.delete(entry) },
                                        onEdit = { onOpenEntry(it.id) }
                                    ) {
                                        DiaryTimelineItem(
                                            entry = entry,
                                            isLast = index == entries.lastIndex,
                                            onClick = { onOpenEntry(entry.id) }
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
    } // end outer Box
}

@Composable
private fun DiaryTimelineItem(
    entry: DiaryEntry,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Left column — 40dp wide: dot + vertical line
        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right card
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = entry.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateUtils.formatRelative(entry.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.content.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    val preview = android.text.Html.fromHtml(
                        entry.content, android.text.Html.FROM_HTML_MODE_COMPACT
                    ).toString().trim()
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun PillMonthHeader(millis: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),  // full pill
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = DateUtils.formatMonth(millis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DiaryCalendarView(
    currentMonthStart: Long,
    entryDates: Set<Long>,
    selectedDate: LocalDate? = null,
    pendingDate: LocalDate? = null,
    onDayClick: (LocalDate) -> Unit,
    onConfirm: (() -> Unit)? = null
) {
    val zone = ZoneId.systemDefault()
    val monthDate = Instant.ofEpochMilli(currentMonthStart).atZone(zone).toLocalDate()
    val daysInMonth = monthDate.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Month title
        Text(
            text = DateUtils.formatMonth(currentMonthStart),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Use ISO week start Monday; dayOfWeek.value: Mon=1,...,Sun=7
        val startOffset = monthDate.dayOfWeek.value - 1  // 0=Monday

        LazyVerticalGrid(
            columns = Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            // Leading empty cells
            items(startOffset) { Box(Modifier.aspectRatio(1f)) }

            // Day cells
            items(daysInMonth) { idx ->
                val dayNum = idx + 1
                val dayDate = monthDate.withDayOfMonth(dayNum)
                val dayMillis = dayDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val hasEntry = entryDates.contains(dayMillis)
                val isToday = dayDate == LocalDate.now(zone)
                val isSelected = selectedDate == dayDate
                val isPending = pendingDate == dayDate

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isPending  -> MaterialTheme.colorScheme.secondary
                                isToday    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else       -> Color.Transparent
                            }
                        )
                        .then(
                            if (hasEntry) Modifier.clickable { onDayClick(dayDate) } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayNum.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isPending  -> MaterialTheme.colorScheme.onSecondary
                                isToday    -> MaterialTheme.colorScheme.primary
                                else       -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (hasEntry) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        // Confirm button row
        if (onConfirm != null && pendingDate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onConfirm) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Confirm date filter",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Group a list of DiaryEntry by month start millis
private fun List<DiaryEntry>.groupByMonth(): Map<Long, List<DiaryEntry>> {
    val result = LinkedHashMap<Long, MutableList<DiaryEntry>>()
    forEach { entry ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = entry.date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val key = cal.timeInMillis
        result.getOrPut(key) { mutableListOf() }.add(entry)
    }
    return result
}
