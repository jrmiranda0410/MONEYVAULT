package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accounting.CurrencyFormatter
import com.example.data.entity.*
import com.example.data.repository.MoneyVaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DashboardUiState(
    val totalBalance: Long = 0L,
    val bankBalance: Long = 0L,
    val cashBalance: Long = 0L,
    val accounts: List<AccountEntity> = emptyList(),
    val todayIncome: Long = 0L,
    val todayExpense: Long = 0L,
    val todayNet: Long = 0L,
    val monthIncome: Long = 0L,
    val monthExpense: Long = 0L,
    val monthNet: Long = 0L,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val currencySymbol: String = "₹"
)

data class HistoryFilterState(
    val searchQuery: String = "",
    val typeFilter: String? = null, // null for All, or "INCOME", "EXPENSE", "TRANSFER"
    val accountIdFilter: Long? = null,
    val categoryIdFilter: Long? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

data class DailyReport(
    val date: LocalDate,
    val openingBalance: Long,
    val income: Long,
    val expenses: Long,
    val transfers: Long,
    val netChange: Long,
    val closingBalance: Long,
    val transactions: List<TransactionEntity>
)

data class CategorySpending(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: Long,
    val amount: Long,
    val percentage: Float
)

data class MonthlyReport(
    val yearMonth: YearMonth,
    val income: Long,
    val expenses: Long,
    val netSavings: Long,
    val bankChange: Long,
    val cashChange: Long,
    val categorySpending: List<CategorySpending>,
    val prevMonthIncome: Long,
    val prevMonthExpenses: Long,
    val prevMonthNetSavings: Long
)

data class BudgetWithProgress(
    val budget: BudgetEntity,
    val categoryName: String?,
    val spent: Long,
    val remaining: Long,
    val percentage: Float // e.g. 75.0f, 100.0f
)

data class MonthlyTrendItem(
    val monthLabel: String,
    val yearMonth: YearMonth,
    val income: Long,
    val expense: Long,
    val net: Long
)

data class DailyTrendItem(
    val dayLabel: String,
    val epochDay: Long,
    val expense: Long
)

class MoneyVaultViewModel(
    private val repository: MoneyVaultRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity?> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History filters
    private val _historyFilter = MutableStateFlow(HistoryFilterState())
    val historyFilter: StateFlow<HistoryFilterState> = _historyFilter.asStateFlow()

    // Filtered transactions stream
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _historyFilter
    ) { transactions, filter ->
        transactions.filter { tx ->
            val matchesQuery = if (filter.searchQuery.isBlank()) true else {
                val q = filter.searchQuery.trim().lowercase()
                tx.description.lowercase().contains(q) ||
                        tx.merchantOrSource.lowercase().contains(q) ||
                        tx.notes.lowercase().contains(q) ||
                        CurrencyFormatter.formatPaise(tx.amount).contains(q)
            }
            val matchesType = filter.typeFilter == null || tx.type == filter.typeFilter
            val matchesAccount = filter.accountIdFilter == null ||
                    tx.accountId == filter.accountIdFilter ||
                    tx.toAccountId == filter.accountIdFilter
            val matchesCategory = filter.categoryIdFilter == null || tx.categoryId == filter.categoryIdFilter
            val matchesStart = filter.startDate == null || tx.dateEpochDay >= filter.startDate
            val matchesEnd = filter.endDate == null || tx.dateEpochDay <= filter.endDate

            matchesQuery && matchesType && matchesAccount && matchesCategory && matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard UI state
    val dashboardState: StateFlow<DashboardUiState> = combine(
        accounts,
        allTransactions,
        userSettings
    ) { accList, txList, settings ->
        val totalBal = accList.sumOf { it.currentBalance }
        val bankAcc = accList.firstOrNull { it.type == AccountType.BANK.name || it.name.equals("Bank", true) }
        val cashAcc = accList.firstOrNull { it.type == AccountType.CASH.name || it.name.equals("Cash", true) }

        val todayEpoch = LocalDate.now().toEpochDay()
        val currentYearMonth = YearMonth.now()
        val startOfMonthEpoch = currentYearMonth.atDay(1).toEpochDay()
        val endOfMonthEpoch = currentYearMonth.atEndOfMonth().toEpochDay()

        val activeTxs = txList.filter { !it.isVoided }

        val todayTxs = activeTxs.filter { it.dateEpochDay == todayEpoch }
        val todayInc = todayTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val todayExp = todayTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

        val monthTxs = activeTxs.filter { it.dateEpochDay in startOfMonthEpoch..endOfMonthEpoch }
        val monthInc = monthTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val monthExp = monthTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

        DashboardUiState(
            totalBalance = totalBal,
            bankBalance = bankAcc?.currentBalance ?: 0L,
            cashBalance = cashAcc?.currentBalance ?: 0L,
            accounts = accList,
            todayIncome = todayInc,
            todayExpense = todayExp,
            todayNet = todayInc - todayExp,
            monthIncome = monthInc,
            monthExpense = monthExp,
            monthNet = monthInc - monthExp,
            recentTransactions = txList.take(6),
            currencySymbol = settings?.currencySymbol ?: "₹"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // Selected Date for Daily Report
    private val _selectedDailyDate = MutableStateFlow(LocalDate.now())
    val selectedDailyDate: StateFlow<LocalDate> = _selectedDailyDate.asStateFlow()

    // Selected YearMonth for Monthly Report
    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()

    // Daily Report reactive state
    val dailyReport: StateFlow<DailyReport> = combine(
        _selectedDailyDate,
        allTransactions,
        allAccounts
    ) { date, txList, accList ->
        val epoch = date.toEpochDay()
        val activeTxs = txList.filter { !it.isVoided }

        // Day transactions
        val dayTxs = activeTxs.filter { it.dateEpochDay == epoch }
        val inc = dayTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val exp = dayTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val transfers = dayTxs.filter { it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }
        val net = inc - exp

        // Prior net changes before this day
        val priorInc = activeTxs.filter { it.dateEpochDay < epoch && it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val priorExp = activeTxs.filter { it.dateEpochDay < epoch && it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val initialOpening = accList.sumOf { it.openingBalance }
        val opening = initialOpening + priorInc - priorExp
        val closing = opening + net

        DailyReport(
            date = date,
            openingBalance = opening,
            income = inc,
            expenses = exp,
            transfers = transfers,
            netChange = net,
            closingBalance = closing,
            transactions = txList.filter { it.dateEpochDay == epoch }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyReport(LocalDate.now(), 0L, 0L, 0L, 0L, 0L, 0L, emptyList()))

    // Monthly Report reactive state
    val monthlyReport: StateFlow<MonthlyReport> = combine(
        _selectedYearMonth,
        allTransactions,
        categories,
        allAccounts
    ) { ym, txList, catList, accList ->
        val startEpoch = ym.atDay(1).toEpochDay()
        val endEpoch = ym.atEndOfMonth().toEpochDay()
        val activeTxs = txList.filter { !it.isVoided }

        val monthTxs = activeTxs.filter { it.dateEpochDay in startEpoch..endEpoch }
        val inc = monthTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val exp = monthTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val net = inc - exp

        // Bank vs cash change in month
        val bankAcc = accList.firstOrNull { it.type == AccountType.BANK.name || it.name.equals("Bank", true) }
        val cashAcc = accList.firstOrNull { it.type == AccountType.CASH.name || it.name.equals("Cash", true) }

        val bankChange = if (bankAcc != null) {
            val bankInc = monthTxs.filter { it.accountId == bankAcc.id && it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val bankExp = monthTxs.filter { it.accountId == bankAcc.id && it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val bankTIn = monthTxs.filter { it.toAccountId == bankAcc.id && it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }
            val bankTOut = monthTxs.filter { it.accountId == bankAcc.id && it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }
            bankInc - bankExp + bankTIn - bankTOut
        } else 0L

        val cashChange = if (cashAcc != null) {
            val cashInc = monthTxs.filter { it.accountId == cashAcc.id && it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val cashExp = monthTxs.filter { it.accountId == cashAcc.id && it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val cashTIn = monthTxs.filter { it.toAccountId == cashAcc.id && it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }
            val cashTOut = monthTxs.filter { it.accountId == cashAcc.id && it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }
            cashInc - cashExp + cashTIn - cashTOut
        } else 0L

        // Category spending
        val categoryMap = catList.associateBy { it.id }
        val expensesByCategory = monthTxs
            .filter { it.type == TransactionType.EXPENSE.name }
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val sum = list.sumOf { it.amount }
                val cat = catId?.let { categoryMap[it] }
                val catName = cat?.name ?: if (list.firstOrNull()?.merchantOrSource?.isNotBlank() == true) list.first().merchantOrSource else "Uncategorized"
                val color = cat?.colorHex ?: 0xFF78909C
                val pct = if (exp > 0) (sum.toFloat() / exp.toFloat()) * 100f else 0f
                CategorySpending(catId, catName, color, sum, pct)
            }
            .sortedByDescending { it.amount }

        // Previous month comparison
        val prevYm = ym.minusMonths(1)
        val prevStart = prevYm.atDay(1).toEpochDay()
        val prevEnd = prevYm.atEndOfMonth().toEpochDay()
        val prevTxs = activeTxs.filter { it.dateEpochDay in prevStart..prevEnd }
        val prevInc = prevTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val prevExp = prevTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val prevNet = prevInc - prevExp

        MonthlyReport(
            yearMonth = ym,
            income = inc,
            expenses = exp,
            netSavings = net,
            bankChange = bankChange,
            cashChange = cashChange,
            categorySpending = expensesByCategory,
            prevMonthIncome = prevInc,
            prevMonthExpenses = prevExp,
            prevMonthNetSavings = prevNet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyReport(YearMonth.now(), 0L, 0L, 0L, 0L, 0L, emptyList(), 0L, 0L, 0L))

    // Budgets with live progress
    val monthlyBudgets: StateFlow<List<BudgetWithProgress>> = combine(
        _selectedYearMonth,
        allTransactions,
        categories
    ) { ym, txList, catList ->
        val budgets = repository.budgetDao.getBudgetsForMonthSync(ym.monthValue, ym.year)
        val startEpoch = ym.atDay(1).toEpochDay()
        val endEpoch = ym.atEndOfMonth().toEpochDay()
        val monthExpenses = txList.filter { !it.isVoided && it.type == TransactionType.EXPENSE.name && it.dateEpochDay in startEpoch..endEpoch }
        val totalMonthlyExpense = monthExpenses.sumOf { it.amount }
        val categoryMap = catList.associateBy { it.id }

        budgets.map { b ->
            val spent = if (b.categoryId == null) {
                totalMonthlyExpense
            } else {
                monthExpenses.filter { it.categoryId == b.categoryId }.sumOf { it.amount }
            }
            val remaining = (b.monthlyLimit - spent).coerceAtLeast(0L)
            val pct = if (b.monthlyLimit > 0) (spent.toFloat() / b.monthlyLimit.toFloat()) * 100f else 0f
            val catName = if (b.categoryId == null) "Overall Budget" else categoryMap[b.categoryId]?.name ?: "Category"

            BudgetWithProgress(
                budget = b,
                categoryName = catName,
                spent = spent,
                remaining = remaining,
                percentage = pct
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics: Monthly trends for last 6 months
    val monthlyTrends: StateFlow<List<MonthlyTrendItem>> = allTransactions.map { txList ->
        val activeTxs = txList.filter { !it.isVoided }
        val current = YearMonth.now()
        (5 downTo 0).map { monthsAgo ->
            val ym = current.minusMonths(monthsAgo.toLong())
            val start = ym.atDay(1).toEpochDay()
            val end = ym.atEndOfMonth().toEpochDay()
            val monthTxs = activeTxs.filter { it.dateEpochDay in start..end }
            val inc = monthTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            MonthlyTrendItem(
                monthLabel = ym.month.name.take(3),
                yearMonth = ym,
                income = inc,
                expense = exp,
                net = inc - exp
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics: Daily expense trend for last 7 days
    val dailyExpenseTrends: StateFlow<List<DailyTrendItem>> = allTransactions.map { txList ->
        val activeTxs = txList.filter { !it.isVoided && it.type == TransactionType.EXPENSE.name }
        val today = LocalDate.now()
        (6 downTo 0).map { daysAgo ->
            val d = today.minusDays(daysAgo.toLong())
            val epoch = d.toEpochDay()
            val dayExpense = activeTxs.filter { it.dateEpochDay == epoch }.sumOf { it.amount }
            DailyTrendItem(
                dayLabel = "${d.dayOfMonth} ${d.month.name.take(3)}",
                epochDay = epoch,
                expense = dayExpense
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Integrity report state
    private val _integrityReport = MutableStateFlow<MoneyVaultRepository.IntegrityReport?>(null)
    val integrityReport: StateFlow<MoneyVaultRepository.IntegrityReport?> = _integrityReport.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeIfEmpty()
        }
    }

    // Actions
    fun setDailyDate(date: LocalDate) {
        _selectedDailyDate.value = date
    }

    fun setYearMonth(ym: YearMonth) {
        _selectedYearMonth.value = ym
    }

    fun updateHistoryFilter(update: (HistoryFilterState) -> HistoryFilterState) {
        _historyFilter.update(update)
    }

    fun recordIncome(
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        dateEpochDay: Long,
        timeFormatted: String,
        source: String,
        description: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.recordIncome(amount, accountId, categoryId, dateEpochDay, timeFormatted, source, description, notes)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to record income")
            }
        }
    }

    fun recordExpense(
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        dateEpochDay: Long,
        timeFormatted: String,
        merchant: String,
        description: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.recordExpense(amount, accountId, categoryId, dateEpochDay, timeFormatted, merchant, description, notes)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to record expense")
            }
        }
    }

    fun recordTransfer(
        amount: Long,
        fromAccountId: Long,
        toAccountId: Long,
        dateEpochDay: Long,
        timeFormatted: String,
        description: String,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.recordTransfer(amount, fromAccountId, toAccountId, dateEpochDay, timeFormatted, description, notes)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to record transfer")
            }
        }
    }

    fun editTransaction(
        txId: Long,
        newAmount: Long,
        newAccountId: Long,
        newToAccountId: Long?,
        newCategoryId: Long?,
        newDateEpochDay: Long,
        newTimeFormatted: String,
        newMerchantOrSource: String,
        newDescription: String,
        newNotes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.editTransaction(
                    txId, newAmount, newAccountId, newToAccountId, newCategoryId,
                    newDateEpochDay, newTimeFormatted, newMerchantOrSource, newDescription, newNotes
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to edit transaction")
            }
        }
    }

    fun voidTransaction(txId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.voidTransaction(txId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to void transaction")
            }
        }
    }

    fun createAccount(name: String, type: String, openingBalance: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.createAccount(name, type, openingBalance)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to create account")
            }
        }
    }

    fun updateAccount(id: Long, name: String, type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateAccount(id, name, type)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update account")
            }
        }
    }

    fun toggleAccountArchive(id: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.toggleAccountArchive(id, isArchived)
        }
    }

    fun createCategory(name: String, type: String, colorHex: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.createCategory(name, type, colorHex)
            onSuccess()
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun setBudget(categoryId: Long?, limit: Long, month: Int, year: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.setBudget(categoryId, limit, month, year)
            onSuccess()
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun updateSettings(
        themeMode: String? = null,
        currencySymbol: String? = null,
        biometricEnabled: Boolean? = null,
        notificationsEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateSettings(themeMode, currencySymbol, biometricEnabled, notificationsEnabled)
        }
    }

    fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = repository.changePin(oldPin, newPin)
            if (success) {
                onResult(true, "PIN successfully changed.")
            } else {
                onResult(false, "Current PIN incorrect.")
            }
        }
    }

    fun runIntegrityCheck() {
        viewModelScope.launch {
            val report = repository.checkIntegrity()
            _integrityReport.value = report
        }
    }

    fun exportCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.generateCsvData()
            onReady(csv)
        }
    }
}
