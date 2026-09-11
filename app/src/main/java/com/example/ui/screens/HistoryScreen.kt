package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.viewmodel.HistoryFilterState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    filterState: HistoryFilterState,
    currencySymbol: String,
    onFilterChange: ((HistoryFilterState) -> HistoryFilterState) -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    var showAccountFilterDialog by remember { mutableStateOf(false) }
    var showCategoryFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { q ->
                    onFilterChange { it.copy(searchQuery = q) }
                },
                placeholder = { Text("Search by description, merchant, notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onFilterChange { it.copy(searchQuery = "") } }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("input_history_search"),
                shape = RoundedCornerShape(12.dp)
            )

            // Horizontal Filter Chips: Type, Account, Category, Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Types
                FilterChip(
                    selected = filterState.typeFilter == null,
                    onClick = { onFilterChange { it.copy(typeFilter = null) } },
                    label = { Text("All") },
                    modifier = Modifier.testTag("chip_filter_all")
                )

                // Income
                FilterChip(
                    selected = filterState.typeFilter == TransactionType.INCOME.name,
                    onClick = {
                        onFilterChange {
                            it.copy(typeFilter = if (it.typeFilter == TransactionType.INCOME.name) null else TransactionType.INCOME.name)
                        }
                    },
                    label = { Text("Income") },
                    modifier = Modifier.testTag("chip_filter_income")
                )

                // Expense
                FilterChip(
                    selected = filterState.typeFilter == TransactionType.EXPENSE.name,
                    onClick = {
                        onFilterChange {
                            it.copy(typeFilter = if (it.typeFilter == TransactionType.EXPENSE.name) null else TransactionType.EXPENSE.name)
                        }
                    },
                    label = { Text("Expense") },
                    modifier = Modifier.testTag("chip_filter_expense")
                )

                // Transfer
                FilterChip(
                    selected = filterState.typeFilter == TransactionType.TRANSFER.name,
                    onClick = {
                        onFilterChange {
                            it.copy(typeFilter = if (it.typeFilter == TransactionType.TRANSFER.name) null else TransactionType.TRANSFER.name)
                        }
                    },
                    label = { Text("Transfer") },
                    modifier = Modifier.testTag("chip_filter_transfer")
                )

                // Account Filter Chip
                val selectedAccount = accountsMap[filterState.accountIdFilter]
                AssistChip(
                    onClick = { showAccountFilterDialog = true },
                    label = { Text(selectedAccount?.name ?: "Account: All") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (filterState.accountIdFilter != null) {
                            IconButton(
                                onClick = { onFilterChange { it.copy(accountIdFilter = null) } },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )

                // Category Filter Chip
                val selectedCategory = categoriesMap[filterState.categoryIdFilter]
                AssistChip(
                    onClick = { showCategoryFilterDialog = true },
                    label = { Text(selectedCategory?.name ?: "Category: All") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (filterState.categoryIdFilter != null) {
                            IconButton(
                                onClick = { onFilterChange { it.copy(categoryIdFilter = null) } },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )

                // Date Filter Chip
                AssistChip(
                    onClick = {
                        val today = LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val date = LocalDate.of(y, m + 1, d)
                                onFilterChange { it.copy(startDate = date.toEpochDay(), endDate = date.toEpochDay()) }
                            },
                            today.year,
                            today.monthValue - 1,
                            today.dayOfMonth
                        ).show()
                    },
                    label = {
                        Text(
                            if (filterState.startDate != null) {
                                LocalDate.ofEpochDay(filterState.startDate).toString()
                            } else "Date: Any"
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (filterState.startDate != null) {
                            IconButton(
                                onClick = { onFilterChange { it.copy(startDate = null, endDate = null) } },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found matching your criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            accountsMap = accountsMap,
                            categoriesMap = categoriesMap,
                            currencySymbol = currencySymbol,
                            onClick = { onTransactionClick(tx.id) }
                        )
                    }
                }
            }
        }
    }

    // Account Filter Dialog
    if (showAccountFilterDialog) {
        AlertDialog(
            onDismissRequest = { showAccountFilterDialog = false },
            title = { Text("Filter by Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onFilterChange { it.copy(accountIdFilter = null) }
                            showAccountFilterDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("All Accounts", modifier = Modifier.fillMaxWidth())
                    }
                    accounts.forEach { acc ->
                        TextButton(
                            onClick = {
                                onFilterChange { it.copy(accountIdFilter = acc.id) }
                                showAccountFilterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${acc.name} (${acc.type})", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Category Filter Dialog
    if (showCategoryFilterDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryFilterDialog = false },
            title = { Text("Filter by Category") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TextButton(
                            onClick = {
                                onFilterChange { it.copy(categoryIdFilter = null) }
                                showCategoryFilterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("All Categories", modifier = Modifier.fillMaxWidth())
                        }
                    }
                    items(categories) { cat ->
                        TextButton(
                            onClick = {
                                onFilterChange { it.copy(categoryIdFilter = cat.id) }
                                showCategoryFilterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${cat.name} (${cat.type})", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
