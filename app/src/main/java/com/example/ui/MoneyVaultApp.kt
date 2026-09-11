package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.security.BiometricHelper
import com.example.ui.screens.*
import com.example.ui.viewmodel.LockViewModel
import com.example.ui.viewmodel.MoneyVaultViewModel
import java.time.LocalDate

sealed interface ScreenDestination {
    data object Dashboard : ScreenDestination
    data object History : ScreenDestination
    data object Accounts : ScreenDestination
    data object Budgets : ScreenDestination
    data object Reports : ScreenDestination
    data object Analytics : ScreenDestination
    data object Settings : ScreenDestination
    data class AddTransaction(val type: String) : ScreenDestination
    data class EditTransaction(val txId: Long) : ScreenDestination
    data class DuplicateTransaction(val txId: Long) : ScreenDestination
    data class TransactionDetails(val txId: Long) : ScreenDestination
}

@Composable
fun MoneyVaultApp(
    lockViewModel: LockViewModel,
    vaultViewModel: MoneyVaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val lockState by lockViewModel.uiState.collectAsStateWithLifecycle()
    val dashboardState by vaultViewModel.dashboardState.collectAsStateWithLifecycle()
    val allTransactions by vaultViewModel.allTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by vaultViewModel.filteredTransactions.collectAsStateWithLifecycle()
    val historyFilter by vaultViewModel.historyFilter.collectAsStateWithLifecycle()
    val allAccounts by vaultViewModel.allAccounts.collectAsStateWithLifecycle()
    val categories by vaultViewModel.categories.collectAsStateWithLifecycle()
    val userSettings by vaultViewModel.userSettings.collectAsStateWithLifecycle()
    val dailyReport by vaultViewModel.dailyReport.collectAsStateWithLifecycle()
    val monthlyReport by vaultViewModel.monthlyReport.collectAsStateWithLifecycle()
    val monthlyBudgets by vaultViewModel.monthlyBudgets.collectAsStateWithLifecycle()
    val monthlyTrends by vaultViewModel.monthlyTrends.collectAsStateWithLifecycle()
    val dailyTrends by vaultViewModel.dailyExpenseTrends.collectAsStateWithLifecycle()
    val integrityReport by vaultViewModel.integrityReport.collectAsStateWithLifecycle()
    val auditLogs by vaultViewModel.auditLogs.collectAsStateWithLifecycle()
    val selectedYearMonth by vaultViewModel.selectedYearMonth.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<ScreenDestination>(ScreenDestination.Dashboard) }

    // If locked, show LockScreen
    if (!lockState.isUnlocked) {
        LockScreen(
            uiState = lockState,
            onDigitEntered = { lockViewModel.onDigitEntered(it) },
            onDelete = { lockViewModel.onDelete() },
            onBiometricClick = {
                if (activity != null) {
                    BiometricHelper.authenticate(
                        activity = activity,
                        onSuccess = { lockViewModel.onBiometricSuccess() },
                        onError = { /* fallback to PIN */ }
                    )
                }
            }
        )
        return
    }

    // Unlocked Navigation BackHandler
    BackHandler(enabled = currentScreen != ScreenDestination.Dashboard) {
        currentScreen = when (currentScreen) {
            is ScreenDestination.TransactionDetails -> ScreenDestination.History
            is ScreenDestination.AddTransaction -> ScreenDestination.Dashboard
            is ScreenDestination.EditTransaction -> {
                val txId = (currentScreen as ScreenDestination.EditTransaction).txId
                ScreenDestination.TransactionDetails(txId)
            }
            is ScreenDestination.DuplicateTransaction -> ScreenDestination.History
            ScreenDestination.Analytics -> ScreenDestination.Dashboard
            ScreenDestination.Accounts -> ScreenDestination.Dashboard
            ScreenDestination.Settings -> ScreenDestination.Dashboard
            else -> ScreenDestination.Dashboard
        }
    }

    // Main scaffold with bottom navigation
    val showBottomBar = currentScreen in listOf(
        ScreenDestination.Dashboard,
        ScreenDestination.History,
        ScreenDestination.Accounts,
        ScreenDestination.Budgets,
        ScreenDestination.Reports,
        ScreenDestination.Settings
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.Dashboard,
                        onClick = { currentScreen = ScreenDestination.Dashboard },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.History,
                        onClick = { currentScreen = ScreenDestination.History },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "History") },
                        label = { Text("History") },
                        modifier = Modifier.testTag("nav_history")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.Accounts,
                        onClick = { currentScreen = ScreenDestination.Accounts },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Accounts") },
                        label = { Text("Accounts") },
                        modifier = Modifier.testTag("nav_accounts")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.Budgets,
                        onClick = { currentScreen = ScreenDestination.Budgets },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = "Budgets") },
                        label = { Text("Budgets") },
                        modifier = Modifier.testTag("nav_budgets")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.Reports || currentScreen == ScreenDestination.Analytics,
                        onClick = { currentScreen = ScreenDestination.Reports },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                        label = { Text("Reports") },
                        modifier = Modifier.testTag("nav_reports")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenDestination.Settings,
                        onClick = { currentScreen = ScreenDestination.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding),
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                ScreenDestination.Dashboard -> {
                    DashboardScreen(
                        dashboardState = dashboardState,
                        categories = categories,
                        onAddIncomeClick = { currentScreen = ScreenDestination.AddTransaction(TransactionType.INCOME.name) },
                        onAddExpenseClick = { currentScreen = ScreenDestination.AddTransaction(TransactionType.EXPENSE.name) },
                        onAddTransferClick = { currentScreen = ScreenDestination.AddTransaction(TransactionType.TRANSFER.name) },
                        onTransactionClick = { txId -> currentScreen = ScreenDestination.TransactionDetails(txId) },
                        onViewAllTransactionsClick = { currentScreen = ScreenDestination.History },
                        onManageAccountsClick = { currentScreen = ScreenDestination.Accounts },
                        onLockClick = { lockViewModel.lockApp() }
                    )
                }

                ScreenDestination.History -> {
                    HistoryScreen(
                        transactions = filteredTransactions,
                        accounts = allAccounts,
                        categories = categories,
                        filterState = historyFilter,
                        currencySymbol = dashboardState.currencySymbol,
                        onFilterChange = { update -> vaultViewModel.updateHistoryFilter(update) },
                        onTransactionClick = { txId -> currentScreen = ScreenDestination.TransactionDetails(txId) }
                    )
                }

                ScreenDestination.Accounts -> {
                    AccountsScreen(
                        accounts = allAccounts,
                        currencySymbol = dashboardState.currencySymbol,
                        onCreateAccount = { name, type, openingPaise ->
                            vaultViewModel.createAccount(
                                name = name,
                                type = type,
                                openingBalance = openingPaise,
                                onSuccess = {
                                    Toast.makeText(context, "Account created", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onUpdateAccount = { id, name, type ->
                            vaultViewModel.updateAccount(
                                id = id,
                                name = name,
                                type = type,
                                onSuccess = {
                                    Toast.makeText(context, "Account updated", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onToggleArchive = { id, isArchived ->
                            vaultViewModel.toggleAccountArchive(id, isArchived)
                        }
                    )
                }

                ScreenDestination.Budgets -> {
                    BudgetsScreen(
                        currentYearMonth = selectedYearMonth,
                        budgets = monthlyBudgets,
                        categories = categories,
                        currencySymbol = dashboardState.currencySymbol,
                        onYearMonthChange = { vaultViewModel.setYearMonth(it) },
                        onSetBudget = { catId, limit, month, year ->
                            vaultViewModel.setBudget(catId, limit, month, year) {
                                Toast.makeText(context, "Budget saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteBudget = { budgetId ->
                            vaultViewModel.deleteBudget(budgetId)
                        }
                    )
                }

                ScreenDestination.Reports -> {
                    ReportsScreen(
                        dailyReport = dailyReport,
                        monthlyReport = monthlyReport,
                        accounts = allAccounts,
                        categories = categories,
                        currencySymbol = dashboardState.currencySymbol,
                        onDailyDateChange = { vaultViewModel.setDailyDate(it) },
                        onMonthlyYearMonthChange = { vaultViewModel.setYearMonth(it) },
                        onTransactionClick = { txId -> currentScreen = ScreenDestination.TransactionDetails(txId) }
                    )
                }

                ScreenDestination.Analytics -> {
                    AnalyticsScreen(
                        monthlyTrends = monthlyTrends,
                        dailyTrends = dailyTrends,
                        categorySpending = monthlyReport.categorySpending,
                        bankBalance = dashboardState.bankBalance,
                        cashBalance = dashboardState.cashBalance,
                        currencySymbol = dashboardState.currencySymbol
                    )
                }

                ScreenDestination.Settings -> {
                    SettingsScreen(
                        userSettings = userSettings,
                        auditLogs = auditLogs,
                        integrityReport = integrityReport,
                        onUpdateTheme = { mode -> vaultViewModel.updateSettings(themeMode = mode) },
                        onUpdateCurrency = { sym -> vaultViewModel.updateSettings(currencySymbol = sym) },
                        onToggleBiometrics = { enabled -> vaultViewModel.updateSettings(biometricEnabled = enabled) },
                        onToggleNotifications = { enabled -> vaultViewModel.updateSettings(notificationsEnabled = enabled) },
                        onChangePin = { oldPin, newPin, callback -> vaultViewModel.changePin(oldPin, newPin, callback) },
                        onRunIntegrityCheck = { vaultViewModel.runIntegrityCheck() },
                        onExportCsv = { callback -> vaultViewModel.exportCsv(callback) },
                        onLockApp = { lockViewModel.lockApp() }
                    )
                }

                is ScreenDestination.AddTransaction -> {
                    AddEditTransactionScreen(
                        initialType = screen.type,
                        accounts = allAccounts.filter { !it.isArchived },
                        categories = categories,
                        currencySymbol = dashboardState.currencySymbol,
                        onSaveIncome = { amount, accId, catId, dateEpoch, timeStr, source, desc, notes ->
                            vaultViewModel.recordIncome(
                                amount, accId, catId, dateEpoch, timeStr, source, desc, notes,
                                onSuccess = {
                                    Toast.makeText(context, "Income recorded", Toast.LENGTH_SHORT).show()
                                    currentScreen = ScreenDestination.Dashboard
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onSaveExpense = { amount, accId, catId, dateEpoch, timeStr, merchant, desc, notes ->
                            vaultViewModel.recordExpense(
                                amount, accId, catId, dateEpoch, timeStr, merchant, desc, notes,
                                onSuccess = {
                                    Toast.makeText(context, "Expense recorded", Toast.LENGTH_SHORT).show()
                                    currentScreen = ScreenDestination.Dashboard
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onSaveTransfer = { amount, fromId, toId, dateEpoch, timeStr, desc, notes ->
                            vaultViewModel.recordTransfer(
                                amount, fromId, toId, dateEpoch, timeStr, desc, notes,
                                onSuccess = {
                                    Toast.makeText(context, "Transfer recorded", Toast.LENGTH_SHORT).show()
                                    currentScreen = ScreenDestination.Dashboard
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onSaveEdit = { _, _, _, _, _, _, _, _, _, _ -> },
                        onBack = { currentScreen = ScreenDestination.Dashboard }
                    )
                }

                is ScreenDestination.EditTransaction -> {
                    val existingTx = allTransactions.firstOrNull { it.id == screen.txId }
                    AddEditTransactionScreen(
                        initialType = existingTx?.type ?: TransactionType.EXPENSE.name,
                        existingTransaction = existingTx,
                        isDuplicate = false,
                        accounts = allAccounts,
                        categories = categories,
                        currencySymbol = dashboardState.currencySymbol,
                        onSaveIncome = { _, _, _, _, _, _, _, _ -> },
                        onSaveExpense = { _, _, _, _, _, _, _, _ -> },
                        onSaveTransfer = { _, _, _, _, _, _, _ -> },
                        onSaveEdit = { txId, amount, accId, toAccId, catId, dateEpoch, timeStr, merchantOrSource, desc, notes ->
                            vaultViewModel.editTransaction(
                                txId, amount, accId, toAccId, catId, dateEpoch, timeStr, merchantOrSource, desc, notes,
                                onSuccess = {
                                    Toast.makeText(context, "Transaction updated", Toast.LENGTH_SHORT).show()
                                    currentScreen = ScreenDestination.TransactionDetails(txId)
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onBack = { currentScreen = ScreenDestination.TransactionDetails(screen.txId) }
                    )
                }

                is ScreenDestination.DuplicateTransaction -> {
                    val originalTx = allTransactions.firstOrNull { it.id == screen.txId }
                    AddEditTransactionScreen(
                        initialType = originalTx?.type ?: TransactionType.EXPENSE.name,
                        existingTransaction = originalTx,
                        isDuplicate = true,
                        accounts = allAccounts.filter { !it.isArchived },
                        categories = categories,
                        currencySymbol = dashboardState.currencySymbol,
                        onSaveIncome = { amount, accId, catId, dateEpoch, timeStr, source, desc, notes ->
                            vaultViewModel.recordIncome(
                                amount, accId, catId, dateEpoch, timeStr, source, desc, notes,
                                onSuccess = {
                                    Toast.makeText(context, "Transaction duplicated", Toast.LENGTH_SHORT).show()
                                    currentScreen = ScreenDestination.History
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onSaveExpense = { amount, accId, catId, dateEpoch, timeStr, merchant, desc, notes ->
                            vaultViewModel.recordExpense(
                                amount, accId, catId, dateEpoch, timeStr, merchant, desc, notes,
                                onSuccess = {
                                    Toast.makeText(context, "Transaction duplicated", Toast.LENGTH_SHORT).show()
                                    currentScreen = ScreenDestination.History
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onSaveTransfer = { amount, fromId, toId, dateEpoch, timeStr, desc, notes ->
                            vaultViewModel.recordTransfer(
                                amount, fromId, toId, dateEpoch, timeStr, desc, notes,
                                onSuccess = {
                                    Toast.makeText(context, "Transfer duplicated", Toast.LENGTH_SHORT).show()
                                    currentScreen = ScreenDestination.History
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onSaveEdit = { _, _, _, _, _, _, _, _, _, _ -> },
                        onBack = { currentScreen = ScreenDestination.TransactionDetails(screen.txId) }
                    )
                }

                is ScreenDestination.TransactionDetails -> {
                    TransactionDetailsScreen(
                        transactionId = screen.txId,
                        transactions = allTransactions,
                        accounts = allAccounts,
                        categories = categories,
                        currencySymbol = dashboardState.currencySymbol,
                        onEditClick = { id -> currentScreen = ScreenDestination.EditTransaction(id) },
                        onDuplicateClick = { id -> currentScreen = ScreenDestination.DuplicateTransaction(id) },
                        onVoidClick = { id ->
                            vaultViewModel.voidTransaction(
                                id,
                                onSuccess = {
                                    Toast.makeText(context, "Transaction marked VOIDED and reversed", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        onBack = { currentScreen = ScreenDestination.History }
                    )
                }
            }
        }
    }
}
