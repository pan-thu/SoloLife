package dev.panthu.sololife.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseCategory
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.hapticReject
import dev.panthu.sololife.util.hapticTick
import dev.panthu.sololife.util.info

@Composable
fun DateGroupHeader(millis: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = DateUtils.formatMonth(millis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            icon()
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200),
                label = "swipeColor"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        },
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
fun AmountText(
    amount: Double,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.tertiary
) {
    Text(
        text = "$${"%.2f".format(amount)}",
        style = style,
        color = color,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun AnimatedAmountText(amount: Double, style: TextStyle, color: Color) {
    val animated by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "amountCounter"
    )
    Text(
        text = "$${"%.2f".format(animated)}",
        style = style,
        color = color,
        fontWeight = FontWeight.ExtraBold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeActionsContainer(
    item: T,
    onDelete: () -> Unit,
    onEdit: (T) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val view = LocalView.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { view.hapticTick(); onEdit(item); false }
                SwipeToDismissBoxValue.EndToStart -> { view.hapticReject(); onDelete(); true }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val targetColor = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                else -> Color.Transparent
            }
            val color by animateColorAsState(
                targetValue = targetColor,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "swipeBg"
            )
            val isEdit = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = if (isEdit) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        content = content
    )
}

@Composable
fun CategoryBreakdownBar(
    allExpenses: List<Expense>,
    selectedCategory: ExpenseCategory?,
    onCategorySelected: (ExpenseCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (allExpenses.isEmpty()) return

    // Calculate per-category totals
    val categoryTotals = ExpenseCategory.entries.mapNotNull { cat ->
        val total = allExpenses.filter { it.category == cat.name }.sumOf { it.amount }
        if (total > 0.0) cat to total else null
    }
    val grandTotal = categoryTotals.sumOf { it.second }
    if (grandTotal == 0.0) return

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        // Segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            categoryTotals.forEachIndexed { index, (cat, total) ->
                val fraction = (total / grandTotal).toFloat()
                val isSelected = selectedCategory == null || selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) cat.info().color
                            else cat.info().color.copy(alpha = 0.3f)
                        )
                        .then(
                            if (index < categoryTotals.lastIndex)
                                Modifier.padding(end = 2.dp) else Modifier
                        )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Filter chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categoryTotals) { (cat, _) ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(if (isSelected) null else cat) },
                    label = { Text(cat.info().label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cat.info().color.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        selectedBorderColor = cat.info().color,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }
    }
}
