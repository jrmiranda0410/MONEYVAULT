package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accounting.CurrencyFormatter
import com.example.ui.theme.*
import com.example.ui.viewmodel.CategorySpending
import com.example.ui.viewmodel.DailyTrendItem
import com.example.ui.viewmodel.MonthlyTrendItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    monthlyTrends: List<MonthlyTrendItem>,
    dailyTrends: List<DailyTrendItem>,
    categorySpending: List<CategorySpending>,
    bankBalance: Long,
    cashBalance: Long,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Trends", fontWeight = FontWeight.Bold) }
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
            // 1. Bank vs Cash Balance Ratio
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_bank_vs_cash"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "BANK VS CASH RATIO",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )

                        val total = (bankBalance + cashBalance).coerceAtLeast(1L)
                        val bankPct = ((bankBalance.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
                        val cashPct = ((cashBalance.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(bankPct.coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(TransferBlue)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(cashPct.coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(IncomeGreen)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TransferBlue))
                                Text("Bank: ${CurrencyFormatter.formatPaise(bankBalance, currencySymbol)} (${String.format("%.0f", bankPct)}%)", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(IncomeGreen))
                                Text("Cash: ${CurrencyFormatter.formatPaise(cashBalance, currencySymbol)} (${String.format("%.0f", cashPct)}%)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // 2. Monthly Income vs Expense Chart (Last 6 Months)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_income_vs_expense"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "MONTHLY INCOME VS EXPENSES (6 MONTHS)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(IncomeGreen))
                                Text("Income", style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ExpenseRed))
                                Text("Expenses", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Bar Chart Canvas
                        MonthlyBarChart(trends = monthlyTrends)
                    }
                }
            }

            // 3. Daily Expense Trend (Last 7 Days)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_daily_expense_trend"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DAILY EXPENSE TREND (LAST 7 DAYS)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )

                        DailyTrendChart(dailyTrends = dailyTrends)
                    }
                }
            }

            // 4. Monthly Savings Trend (Net Savings over 6 months)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_savings_trend"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SAVINGS TREND (NET ACCUMULATION)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )

                        monthlyTrends.forEach { trend ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(trend.monthLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = "${if (trend.net >= 0) "+" else ""}${CurrencyFormatter.formatPaise(trend.net, currencySymbol)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (trend.net >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // 5. Category Spending Breakdown
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_category_breakdown"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "TOP EXPENSE CATEGORIES",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )

                        if (categorySpending.isEmpty()) {
                            Text("No category expenses recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            categorySpending.take(6).forEach { cs ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cs.categoryName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text(
                                            "${CurrencyFormatter.formatPaise(cs.amount, currencySymbol)} (${String.format("%.1f", cs.percentage)}%)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
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
        }
    }
}

@Composable
private fun MonthlyBarChart(trends: List<MonthlyTrendItem>) {
    val maxVal = trends.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val width = size.width
            val height = size.height
            val count = trends.size.coerceAtLeast(1)
            val groupWidth = width / count
            val barWidth = (groupWidth * 0.35f).coerceAtMost(24.dp.toPx())
            val spacing = 4.dp.toPx()

            trends.forEachIndexed { index, item ->
                val centerX = index * groupWidth + (groupWidth / 2f)

                // Income bar
                val incHeight = (item.income.toFloat() / maxVal.toFloat()) * height
                val incLeft = centerX - barWidth - (spacing / 2f)
                val incTop = height - incHeight
                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(incLeft, incTop),
                    size = Size(barWidth, incHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Expense bar
                val expHeight = (item.expense.toFloat() / maxVal.toFloat()) * height
                val expLeft = centerX + (spacing / 2f)
                val expTop = height - expHeight
                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(expLeft, expTop),
                    size = Size(barWidth, expHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            trends.forEach {
                Text(text = it.monthLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DailyTrendChart(dailyTrends: List<DailyTrendItem>) {
    val maxVal = dailyTrends.maxOfOrNull { it.expense }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val width = size.width
            val height = size.height
            val count = dailyTrends.size.coerceAtLeast(1)
            val stepX = if (count > 1) width / (count - 1) else width

            val path = Path()
            dailyTrends.forEachIndexed { index, item ->
                val x = index * stepX
                val y = height - ((item.expense.toFloat() / maxVal.toFloat()) * (height - 10f)) - 5f
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = ExpenseRed,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw dots
            dailyTrends.forEachIndexed { index, item ->
                val x = index * stepX
                val y = height - ((item.expense.toFloat() / maxVal.toFloat()) * (height - 10f)) - 5f
                drawCircle(
                    color = ExpenseRed,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyTrends.forEach {
                Text(text = it.dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
