package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accounting.CurrencyFormatter
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CategoryType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExceededRed
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.BudgetWithProgress
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    currentYearMonth: YearMonth,
    budgets: List<BudgetWithProgress>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onYearMonthChange: (YearMonth) -> Unit,
    onSetBudget: (categoryId: Long?, limitPaise: Long, month: Int, year: Int) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSetBudgetDialog by remember { mutableStateOf(false) }

    val expenseCategories = remember(categories) {
        categories.filter { it.type == CategoryType.EXPENSE.name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets & Limits", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showSetBudgetDialog = true },
                        modifier = Modifier.testTag("btn_add_budget")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Budget")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Switcher Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onYearMonthChange(currentYearMonth.minusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Text(
                        text = "${currentYearMonth.month.name} ${currentYearMonth.year}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    IconButton(onClick = { onYearMonthChange(currentYearMonth.plusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }

            if (budgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No budgets set for this month.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = { showSetBudgetDialog = true }) {
                                    Text("Set Budget Now")
                                }
                            }
                        }
                    }
                }
            } else {
                items(budgets, key = { it.budget.id }) { b ->
                    BudgetCard(
                        item = b,
                        currencySymbol = currencySymbol,
                        onDelete = { onDeleteBudget(b.budget.id) }
                    )
                }
            }
        }
    }

    if (showSetBudgetDialog) {
        SetBudgetDialog(
            expenseCategories = expenseCategories,
            currentYearMonth = currentYearMonth,
            currencySymbol = currencySymbol,
            onDismiss = { showSetBudgetDialog = false },
            onConfirm = { catId, limit ->
                onSetBudget(catId, limit, currentYearMonth.monthValue, currentYearMonth.year)
                showSetBudgetDialog = false
            }
        )
    }
}

@Composable
private fun BudgetCard(
    item: BudgetWithProgress,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    val pct = item.percentage
    val isOverall = item.budget.categoryId == null

    val (alertColor, alertText) = when {
        pct >= 100f -> Pair(ExceededRed, "Exceeded (${String.format("%.1f", pct)}%)")
        pct >= 90f -> Pair(ExpenseRed, "90%+ Critical (${String.format("%.1f", pct)}%)")
        pct >= 75f -> Pair(WarningAmber, "75%+ Warning (${String.format("%.1f", pct)}%)")
        else -> Pair(EmeraldPrimary, "${String.format("%.1f", pct)}% used")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_card_${item.budget.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isOverall) Icons.Default.AccountBalanceWallet else Icons.Default.Category,
                        contentDescription = null,
                        tint = if (isOverall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.categoryName ?: "Overall Budget",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = alertColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = alertText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = alertColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = alertColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Spent, Remaining & Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatPaise(item.spent, currencySymbol), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.formatPaise(item.remaining, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.remaining == 0L && item.spent >= item.budget.monthlyLimit) ExceededRed else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyFormatter.formatPaise(item.budget.monthlyLimit, currencySymbol), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetBudgetDialog(
    expenseCategories: List<CategoryEntity>,
    currentYearMonth: YearMonth,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long?, limitPaise: Long) -> Unit
) {
    var isOverall by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(expenseCategories.firstOrNull()?.id) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "For ${currentYearMonth.month.name} ${currentYearMonth.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isOverall,
                        onClick = { isOverall = true }
                    )
                    Text("Overall Monthly Budget", modifier = Modifier.padding(start = 4.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isOverall,
                        onClick = { isOverall = false }
                    )
                    Text("Specific Category Budget", modifier = Modifier.padding(start = 4.dp))
                }

                if (!isOverall) {
                    val currentCat = expenseCategories.firstOrNull { it.id == selectedCategoryId }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentCat?.name ?: "Select Expense Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            expenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Monthly Limit ($currencySymbol)") },
                    placeholder = { Text("5000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_budget_limit")
                )

                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paise = CurrencyFormatter.parseInputToPaise(limitInput)
                    if (paise == null || paise <= 0) {
                        error = "Please enter a valid positive budget limit."
                        return@Button
                    }
                    val catId = if (isOverall) null else selectedCategoryId
                    onConfirm(catId, paise)
                }
            ) {
                Text("Set Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
