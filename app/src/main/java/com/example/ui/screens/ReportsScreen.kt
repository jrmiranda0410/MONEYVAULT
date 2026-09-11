package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accounting.CurrencyFormatter
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DailyReport
import com.example.ui.viewmodel.MonthlyReport
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    dailyReport: DailyReport,
    monthlyReport: MonthlyReport,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onDailyDateChange: (LocalDate) -> Unit,
    onMonthlyYearMonthChange: (YearMonth) -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Daily, 1: Monthly
    val context = LocalContext.current
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Reports", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Daily Report", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_daily_report")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Monthly Report", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_monthly_report")
                )
            }

            if (selectedTab == 0) {
                // DAILY REPORT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date Navigation
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onDailyDateChange(dailyReport.date.minusDays(1)) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${dailyReport.date.dayOfWeek.name.take(3)}, ${dailyReport.date}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                IconButton(
                                    onClick = {
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                onDailyDateChange(LocalDate.of(y, m + 1, d))
                                            },
                                            dailyReport.date.year,
                                            dailyReport.date.monthValue - 1,
                                            dailyReport.date.dayOfMonth
                                        ).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", modifier = Modifier.size(18.dp))
                                }
                            }

                            IconButton(onClick = { onDailyDateChange(dailyReport.date.plusDays(1)) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                            }
                        }
                    }

                    // Opening & Closing Balance Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("OPENING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CurrencyFormatter.formatPaise(dailyReport.openingBalance, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("CLOSING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CurrencyFormatter.formatPaise(dailyReport.closingBalance, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    // Daily Metrics
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("DAILY SUMMARY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))

                                ReportStatRow("Total Income", CurrencyFormatter.formatPaise(dailyReport.income, currencySymbol), IncomeGreen)
                                ReportStatRow("Total Expenses", CurrencyFormatter.formatPaise(dailyReport.expenses, currencySymbol), ExpenseRed)
                                ReportStatRow("Transfers Volume", CurrencyFormatter.formatPaise(dailyReport.transfers, currencySymbol), TransferBlue)

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                ReportStatRow(
                                    "Net Change",
                                    CurrencyFormatter.formatPaise(dailyReport.netChange, currencySymbol),
                                    if (dailyReport.netChange >= 0) IncomeGreen else ExpenseRed,
                                    isBold = true
                                )
                            }
                        }
                    }

                    // Transactions for this day
                    item {
                        Text(
                            text = "Day's Transactions (${dailyReport.transactions.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (dailyReport.transactions.isEmpty()) {
                        item {
                            Text(
                                "No transactions recorded for this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(dailyReport.transactions, key = { it.id }) { tx ->
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
            } else {
                // MONTHLY REPORT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Month Navigation
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onMonthlyYearMonthChange(monthlyReport.yearMonth.minusMonths(1)) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }

                            Text(
                                text = "${monthlyReport.yearMonth.month.name} ${monthlyReport.yearMonth.year}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            IconButton(onClick = { onMonthlyYearMonthChange(monthlyReport.yearMonth.plusMonths(1)) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }
                    }

                    // Net Savings Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (monthlyReport.netSavings >= 0) MaterialTheme.colorScheme.primaryContainer else ExpenseRed.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "NET SAVINGS THIS MONTH",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaise(monthlyReport.netSavings, currencySymbol),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (monthlyReport.netSavings >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }

                    // Income & Expenses Summary
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("INCOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CurrencyFormatter.formatPaise(monthlyReport.income, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IncomeGreen)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("EXPENSES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        CurrencyFormatter.formatPaise(monthlyReport.expenses, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ExpenseRed)
                                    )
                                }
                            }
                        }
                    }

                    // Bank & Cash Total Change
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("ACCOUNT BALANCES CHANGE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))

                                ReportStatRow(
                                    "Bank Balance Change",
                                    CurrencyFormatter.formatPaise(monthlyReport.bankChange, currencySymbol),
                                    if (monthlyReport.bankChange >= 0) IncomeGreen else ExpenseRed
                                )
                                ReportStatRow(
                                    "Cash Balance Change",
                                    CurrencyFormatter.formatPaise(monthlyReport.cashChange, currencySymbol),
                                    if (monthlyReport.cashChange >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }

                    // Category Breakdown
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("EXPENSES BY CATEGORY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))

                                if (monthlyReport.categorySpending.isEmpty()) {
                                    Text("No expenses recorded for this month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    monthlyReport.categorySpending.forEach { cs ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(cs.colorHex))
                                                    )
                                                    Text(cs.categoryName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                                }

                                                Text(
                                                    "${CurrencyFormatter.formatPaise(cs.amount, currencySymbol)} (${String.format("%.1f", cs.percentage)}%)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }

                                            LinearProgressIndicator(
                                                progress = { (cs.percentage / 100f).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = Color(cs.colorHex),
                                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Previous Month Comparison
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("PREVIOUS MONTH COMPARISON", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))

                                val incDiff = monthlyReport.income - monthlyReport.prevMonthIncome
                                val expDiff = monthlyReport.expenses - monthlyReport.prevMonthExpenses
                                val netDiff = monthlyReport.netSavings - monthlyReport.prevMonthNetSavings

                                ReportStatRow("Previous Month Income", CurrencyFormatter.formatPaise(monthlyReport.prevMonthIncome, currencySymbol))
                                ReportStatRow(
                                    "Income Difference",
                                    "${if (incDiff >= 0) "+" else ""}${CurrencyFormatter.formatPaise(incDiff, currencySymbol)}",
                                    if (incDiff >= 0) IncomeGreen else ExpenseRed
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                ReportStatRow("Previous Month Expenses", CurrencyFormatter.formatPaise(monthlyReport.prevMonthExpenses, currencySymbol))
                                ReportStatRow(
                                    "Expense Difference",
                                    "${if (expDiff >= 0) "+" else ""}${CurrencyFormatter.formatPaise(expDiff, currencySymbol)}",
                                    if (expDiff <= 0) IncomeGreen else ExpenseRed // Lower expense is green!
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                ReportStatRow("Previous Month Savings", CurrencyFormatter.formatPaise(monthlyReport.prevMonthNetSavings, currencySymbol))
                                ReportStatRow(
                                    "Savings Difference",
                                    "${if (netDiff >= 0) "+" else ""}${CurrencyFormatter.formatPaise(netDiff, currencySymbol)}",
                                    if (netDiff >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}
