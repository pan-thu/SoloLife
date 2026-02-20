package dev.panthu.sololife.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseCategory
import dev.panthu.sololife.util.CategoryInfo
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.hapticConfirm
import dev.panthu.sololife.util.hapticTick
import dev.panthu.sololife.util.info
import dev.panthu.sololife.util.toExpenseCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    expense: Expense? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: ExpenseCategory, description: String, date: Long) -> Unit
) {
    var amountText by remember { mutableStateOf(if (expense != null) "%.2f".format(expense.amount) else "") }
    var selectedCategory by remember { mutableStateOf(expense?.category?.toExpenseCategory() ?: ExpenseCategory.FOOD) }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var date by remember { mutableStateOf(expense?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }

    val isEditMode = expense != null
    val title = if (isEditMode) "Edit Expense" else "Add Expense"
    val buttonLabel = if (isEditMode) "Save Changes" else "Add Expense"
    val view = LocalView.current

    // Delay focus request so the sheet window finishes initializing before the keyboard appears,
    // preventing a FocusEvent ANR on the main thread.
    LaunchedEffect(Unit) {
        delay(300)
        amountFocus.requestFocus()
    }

    val isValid = amountText.toDoubleOrNull()?.let { it > 0.0 } == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(20.dp))

            // Amount input — large centered
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    BasicAmountField(
                        value = amountText,
                        onValueChange = { new ->
                            val filtered = new.filter { c -> c.isDigit() || c == '.' }
                            // Block a second decimal point
                            if (filtered.count { it == '.' } <= 1) amountText = filtered
                        },
                        modifier = Modifier
                            .widthIn(min = 80.dp, max = 200.dp)
                            .focusRequester(amountFocus)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Date selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = "Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = DateUtils.formatFull(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Category pills
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(ExpenseCategory.entries) { category ->
                    val info = category.info()
                    val isSelected = category == selectedCategory
                    CategoryPill(
                        info = info,
                        isSelected = isSelected,
                        onClick = { view.hapticTick(); selectedCategory = category }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            Spacer(Modifier.height(28.dp))

            // Save / Add button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    view.hapticConfirm()
                    onSave(amount, selectedCategory, description.trim(), date)
                    onDismiss()
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    buttonLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Date picker dialog (outside ModalBottomSheet to avoid z-order issues)
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun CategoryPill(info: CategoryInfo, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) info.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (isSelected) info.color else androidx.compose.ui.graphics.Color.Transparent

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = info.icon,
            contentDescription = null,
            tint = info.color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = info.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BasicAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = MaterialTheme.typography.displayMedium.fontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        "0.00",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                inner()
            }
        },
        modifier = modifier
    )
}
