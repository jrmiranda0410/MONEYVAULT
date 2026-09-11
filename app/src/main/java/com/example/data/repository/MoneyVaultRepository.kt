package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.accounting.CurrencyFormatter
import com.example.data.database.MoneyVaultDatabase
import com.example.data.entity.*
import com.example.notification.NotificationHelper
import com.example.security.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MoneyVaultRepository(
    private val database: MoneyVaultDatabase,
    private val notificationHelper: NotificationHelper
) {
    val accountDao = database.accountDao()
    val categoryDao = database.categoryDao()
    val transactionDao = database.transactionDao()
    val ledgerDao = database.ledgerDao()
    val budgetDao = database.budgetDao()
    val auditLogDao = database.auditLogDao()
    val userSettingsDao = database.userSettingsDao()

    // Reactive streams
    val activeAccounts: Flow<List<AccountEntity>> = accountDao.getActiveAccounts()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(10)
    val userSettings: Flow<UserSettingsEntity?> = userSettingsDao.getSettingsFlow()
    val auditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()

    suspend fun initializeIfEmpty() = withContext(Dispatchers.IO) {
        MoneyVaultDatabase.prepopulateDatabase(database)
    }

    // ==========================================
    // TRANSACTION CREATION (INCOME, EXPENSE, TRANSFER)
    // ==========================================

    suspend fun recordIncome(
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        dateEpochDay: Long,
        timeFormatted: String,
        source: String,
        description: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val account = accountDao.getAccountById(accountId)
                ?: throw IllegalArgumentException("Account not found")

            val tx = TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = amount,
                accountId = accountId,
                toAccountId = null,
                categoryId = categoryId,
                dateEpochDay = dateEpochDay,
                timeFormatted = timeFormatted,
                merchantOrSource = source,
                description = description,
                notes = notes,
                isVoided = false
            )
            val txId = transactionDao.insertTransaction(tx)

            // Double Entry: Debit Account, Credit Income (Category/Source)
            val category = categoryId?.let { categoryDao.getCategoryById(it) }
            val incomeAccountName = category?.name ?: if (source.isNotBlank()) source else "Income"

            val entries = listOf(
                LedgerEntryEntity(
                    transactionId = txId,
                    accountId = accountId,
                    accountName = account.name,
                    debit = amount,
                    credit = 0L,
                    description = "Deposit into ${account.name}"
                ),
                LedgerEntryEntity(
                    transactionId = txId,
                    accountId = null,
                    accountName = incomeAccountName,
                    debit = 0L,
                    credit = amount,
                    description = "Income from $incomeAccountName"
                )
            )
            ledgerDao.insertAll(entries)

            // Update Account Balance
            val newBalance = account.currentBalance + amount
            accountDao.updateBalance(accountId, newBalance)

            // Audit Log
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "Transaction created",
                    targetId = "TX#$txId",
                    details = "Income of ${CurrencyFormatter.formatPaise(amount)} into ${account.name} ($incomeAccountName)"
                )
            )

            // Notification
            val settings = userSettingsDao.getSettings()
            if (settings?.notificationsEnabled != false) {
                notificationHelper.notifyTransactionRecorded(
                    "Income Recorded",
                    "+${CurrencyFormatter.formatPaise(amount, settings?.currencySymbol ?: "₹")} deposited to ${account.name}"
                )
            }

            txId
        }
    }

    suspend fun recordExpense(
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        dateEpochDay: Long,
        timeFormatted: String,
        merchant: String,
        description: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val account = accountDao.getAccountById(accountId)
                ?: throw IllegalArgumentException("Account not found")

            val tx = TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = amount,
                accountId = accountId,
                toAccountId = null,
                categoryId = categoryId,
                dateEpochDay = dateEpochDay,
                timeFormatted = timeFormatted,
                merchantOrSource = merchant,
                description = description,
                notes = notes,
                isVoided = false
            )
            val txId = transactionDao.insertTransaction(tx)

            // Double Entry: Debit Expense (Category/Merchant), Credit Account
            val category = categoryId?.let { categoryDao.getCategoryById(it) }
            val expenseAccountName = category?.name ?: if (merchant.isNotBlank()) merchant else "Expense"

            val entries = listOf(
                LedgerEntryEntity(
                    transactionId = txId,
                    accountId = null,
                    accountName = expenseAccountName,
                    debit = amount,
                    credit = 0L,
                    description = "Expense for $expenseAccountName"
                ),
                LedgerEntryEntity(
                    transactionId = txId,
                    accountId = accountId,
                    accountName = account.name,
                    debit = 0L,
                    credit = amount,
                    description = "Payment from ${account.name}"
                )
            )
            ledgerDao.insertAll(entries)

            // Update Account Balance
            val newBalance = account.currentBalance - amount
            accountDao.updateBalance(accountId, newBalance)

            // Audit Log
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "Transaction created",
                    targetId = "TX#$txId",
                    details = "Expense of ${CurrencyFormatter.formatPaise(amount)} from ${account.name} ($expenseAccountName)"
                )
            )

            // Check Budgets and Notify
            val settings = userSettingsDao.getSettings()
            val currency = settings?.currencySymbol ?: "₹"
            if (settings?.notificationsEnabled != false) {
                notificationHelper.notifyTransactionRecorded(
                    "Expense Recorded",
                    "-${CurrencyFormatter.formatPaise(amount, currency)} paid from ${account.name} for $expenseAccountName"
                )

                // Check budget alerts
                checkBudgetAlerts(categoryId, dateEpochDay, currency)
            }

            txId
        }
    }

    suspend fun recordTransfer(
        amount: Long,
        fromAccountId: Long,
        toAccountId: Long,
        dateEpochDay: Long,
        timeFormatted: String,
        description: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        if (fromAccountId == toAccountId) {
            throw IllegalArgumentException("Source and destination accounts must be different.")
        }

        database.withTransaction {
            val fromAccount = accountDao.getAccountById(fromAccountId)
                ?: throw IllegalArgumentException("Source account not found")
            val toAccount = accountDao.getAccountById(toAccountId)
                ?: throw IllegalArgumentException("Destination account not found")

            val tx = TransactionEntity(
                type = TransactionType.TRANSFER.name,
                amount = amount,
                accountId = fromAccountId,
                toAccountId = toAccountId,
                categoryId = null,
                dateEpochDay = dateEpochDay,
                timeFormatted = timeFormatted,
                merchantOrSource = "",
                description = description,
                notes = notes,
                isVoided = false
            )
            val txId = transactionDao.insertTransaction(tx)

            // Double Entry: Debit ToAccount (increasing asset), Credit FromAccount (decreasing asset)
            val entries = listOf(
                LedgerEntryEntity(
                    transactionId = txId,
                    accountId = toAccountId,
                    accountName = toAccount.name,
                    debit = amount,
                    credit = 0L,
                    description = "Transfer into ${toAccount.name} from ${fromAccount.name}"
                ),
                LedgerEntryEntity(
                    transactionId = txId,
                    accountId = fromAccountId,
                    accountName = fromAccount.name,
                    debit = 0L,
                    credit = amount,
                    description = "Transfer out of ${fromAccount.name} to ${toAccount.name}"
                )
            )
            ledgerDao.insertAll(entries)

            // Update Account Balances: Total wealth unchanged
            accountDao.updateBalance(fromAccountId, fromAccount.currentBalance - amount)
            accountDao.updateBalance(toAccountId, toAccount.currentBalance + amount)

            // Audit Log
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "Transaction created",
                    targetId = "TX#$txId",
                    details = "Transfer of ${CurrencyFormatter.formatPaise(amount)} from ${fromAccount.name} to ${toAccount.name}"
                )
            )

            // Notification
            val settings = userSettingsDao.getSettings()
            if (settings?.notificationsEnabled != false) {
                notificationHelper.notifyTransactionRecorded(
                    "Transfer Recorded",
                    "Transferred ${CurrencyFormatter.formatPaise(amount, settings?.currencySymbol ?: "₹")} from ${fromAccount.name} to ${toAccount.name}"
                )
            }

            txId
        }
    }

    // ==========================================
    // VOID TRANSACTION (NON-DESTRUCTIVE)
    // ==========================================

    suspend fun voidTransaction(txId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val tx = transactionDao.getTransactionById(txId)
                ?: throw IllegalArgumentException("Transaction not found")
            if (tx.isVoided) return@withTransaction // already voided

            // 1. Mark transaction as voided
            transactionDao.voidTransaction(txId)

            // 2. Reverse accounting balance effect
            when (tx.type) {
                TransactionType.INCOME.name -> {
                    val account = accountDao.getAccountById(tx.accountId)
                    if (account != null) {
                        accountDao.updateBalance(tx.accountId, account.currentBalance - tx.amount)
                    }
                }
                TransactionType.EXPENSE.name -> {
                    val account = accountDao.getAccountById(tx.accountId)
                    if (account != null) {
                        accountDao.updateBalance(tx.accountId, account.currentBalance + tx.amount)
                    }
                }
                TransactionType.TRANSFER.name -> {
                    val fromAccount = accountDao.getAccountById(tx.accountId)
                    val toAccount = tx.toAccountId?.let { accountDao.getAccountById(it) }
                    if (fromAccount != null) {
                        accountDao.updateBalance(tx.accountId, fromAccount.currentBalance + tx.amount)
                    }
                    if (toAccount != null) {
                        accountDao.updateBalance(toAccount.id, toAccount.currentBalance - tx.amount)
                    }
                }
            }

            // 3. Add reversing ledger entries
            val existingEntries = ledgerDao.getEntriesForTransactionSync(txId)
            val reversingEntries = existingEntries.map { entry ->
                LedgerEntryEntity(
                    transactionId = txId,
                    accountId = entry.accountId,
                    accountName = "[VOID] " + entry.accountName,
                    debit = entry.credit, // swap debit and credit to balance out
                    credit = entry.debit,
                    description = "Reversal for voided TX#$txId"
                )
            }
            ledgerDao.insertAll(reversingEntries)

            // 4. Audit Log
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "Transaction voided",
                    targetId = "TX#$txId",
                    details = "Voided ${tx.type} of ${CurrencyFormatter.formatPaise(tx.amount)}. Accounting impact reversed."
                )
            )
        }
    }

    // ==========================================
    // EDIT TRANSACTION (REVERSE OLD + APPLY NEW)
    // ==========================================

    suspend fun editTransaction(
        txId: Long,
        newAmount: Long,
        newAccountId: Long,
        newToAccountId: Long?,
        newCategoryId: Long?,
        newDateEpochDay: Long,
        newTimeFormatted: String,
        newMerchantOrSource: String,
        newDescription: String,
        newNotes: String
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val oldTx = transactionDao.getTransactionById(txId)
                ?: throw IllegalArgumentException("Transaction not found")

            if (oldTx.isVoided) {
                throw IllegalStateException("Cannot edit a voided transaction.")
            }

            // Step 1: Reverse old transaction effect
            when (oldTx.type) {
                TransactionType.INCOME.name -> {
                    val acc = accountDao.getAccountById(oldTx.accountId)
                    if (acc != null) accountDao.updateBalance(acc.id, acc.currentBalance - oldTx.amount)
                }
                TransactionType.EXPENSE.name -> {
                    val acc = accountDao.getAccountById(oldTx.accountId)
                    if (acc != null) accountDao.updateBalance(acc.id, acc.currentBalance + oldTx.amount)
                }
                TransactionType.TRANSFER.name -> {
                    val from = accountDao.getAccountById(oldTx.accountId)
                    val to = oldTx.toAccountId?.let { accountDao.getAccountById(it) }
                    if (from != null) accountDao.updateBalance(from.id, from.currentBalance + oldTx.amount)
                    if (to != null) accountDao.updateBalance(to.id, to.currentBalance - oldTx.amount)
                }
            }

            // Step 2: Apply new transaction effect
            when (oldTx.type) {
                TransactionType.INCOME.name -> {
                    val acc = accountDao.getAccountById(newAccountId)
                        ?: throw IllegalArgumentException("Account not found")
                    accountDao.updateBalance(acc.id, acc.currentBalance + newAmount)
                }
                TransactionType.EXPENSE.name -> {
                    val acc = accountDao.getAccountById(newAccountId)
                        ?: throw IllegalArgumentException("Account not found")
                    accountDao.updateBalance(acc.id, acc.currentBalance - newAmount)
                }
                TransactionType.TRANSFER.name -> {
                    if (newToAccountId == null || newAccountId == newToAccountId) {
                        throw IllegalArgumentException("Source and destination accounts must be different.")
                    }
                    val from = accountDao.getAccountById(newAccountId)
                        ?: throw IllegalArgumentException("Source account not found")
                    val to = accountDao.getAccountById(newToAccountId)
                        ?: throw IllegalArgumentException("Destination account not found")
                    accountDao.updateBalance(from.id, from.currentBalance - newAmount)
                    accountDao.updateBalance(to.id, to.currentBalance + newAmount)
                }
            }

            // Step 3: Update ledger entries
            ledgerDao.deleteEntriesForTransaction(txId)

            val newEntries = when (oldTx.type) {
                TransactionType.INCOME.name -> {
                    val acc = accountDao.getAccountById(newAccountId)!!
                    val category = newCategoryId?.let { categoryDao.getCategoryById(it) }
                    val name = category?.name ?: if (newMerchantOrSource.isNotBlank()) newMerchantOrSource else "Income"
                    listOf(
                        LedgerEntryEntity(
                            transactionId = txId,
                            accountId = newAccountId,
                            accountName = acc.name,
                            debit = newAmount,
                            credit = 0L,
                            description = "Deposit into ${acc.name}"
                        ),
                        LedgerEntryEntity(
                            transactionId = txId,
                            accountId = null,
                            accountName = name,
                            debit = 0L,
                            credit = newAmount,
                            description = "Income from $name"
                        )
                    )
                }
                TransactionType.EXPENSE.name -> {
                    val acc = accountDao.getAccountById(newAccountId)!!
                    val category = newCategoryId?.let { categoryDao.getCategoryById(it) }
                    val name = category?.name ?: if (newMerchantOrSource.isNotBlank()) newMerchantOrSource else "Expense"
                    listOf(
                        LedgerEntryEntity(
                            transactionId = txId,
                            accountId = null,
                            accountName = name,
                            debit = newAmount,
                            credit = 0L,
                            description = "Expense for $name"
                        ),
                        LedgerEntryEntity(
                            transactionId = txId,
                            accountId = newAccountId,
                            accountName = acc.name,
                            debit = 0L,
                            credit = newAmount,
                            description = "Payment from ${acc.name}"
                        )
                    )
                }
                TransactionType.TRANSFER.name -> {
                    val from = accountDao.getAccountById(newAccountId)!!
                    val to = accountDao.getAccountById(newToAccountId!!)!!
                    listOf(
                        LedgerEntryEntity(
                            transactionId = txId,
                            accountId = to.id,
                            accountName = to.name,
                            debit = newAmount,
                            credit = 0L,
                            description = "Transfer into ${to.name}"
                        ),
                        LedgerEntryEntity(
                            transactionId = txId,
                            accountId = from.id,
                            accountName = from.name,
                            debit = 0L,
                            credit = newAmount,
                            description = "Transfer out of ${from.name}"
                        )
                    )
                }
                else -> emptyList()
            }
            ledgerDao.insertAll(newEntries)

            // Step 4: Update transaction
            val updatedTx = oldTx.copy(
                amount = newAmount,
                accountId = newAccountId,
                toAccountId = newToAccountId,
                categoryId = newCategoryId,
                dateEpochDay = newDateEpochDay,
                timeFormatted = newTimeFormatted,
                merchantOrSource = newMerchantOrSource,
                description = newDescription,
                notes = newNotes,
                updatedAt = System.currentTimeMillis()
            )
            transactionDao.updateTransaction(updatedTx)

            // Step 5: Audit Log
            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "Transaction edited",
                    targetId = "TX#$txId",
                    details = "Edited TX#$txId from ${CurrencyFormatter.formatPaise(oldTx.amount)} to ${CurrencyFormatter.formatPaise(newAmount)}. Balances adjusted."
                )
            )
        }
    }

    // ==========================================
    // BUDGET CHECKS & NOTIFICATIONS
    // ==========================================

    private suspend fun checkBudgetAlerts(categoryId: Long?, epochDay: Long, currency: String) {
        val date = LocalDate.ofEpochDay(epochDay)
        val month = date.monthValue
        val year = date.year

        val startOfMonthEpoch = LocalDate.of(year, month, 1).toEpochDay()
        val endOfMonthEpoch = LocalDate.of(year, month, date.lengthOfMonth()).toEpochDay()

        val monthlyTransactions = transactionDao.getTransactionsForDateRangeSync(startOfMonthEpoch, endOfMonthEpoch)
        val expenses = monthlyTransactions.filter { it.type == TransactionType.EXPENSE.name && !it.isVoided }

        // 1. Overall budget check
        val budgets = budgetDao.getBudgetsForMonthSync(month, year)
        val overallBudget = budgets.firstOrNull { it.categoryId == null }
        if (overallBudget != null && overallBudget.monthlyLimit > 0) {
            val totalExpense = expenses.sumOf { it.amount }
            val ratio = (totalExpense.toDouble() / overallBudget.monthlyLimit.toDouble()) * 100
            if (ratio >= 100.0) {
                notificationHelper.notifyBudgetAlert(
                    "Budget Exceeded!",
                    "Overall monthly spending has reached ${ratio.toInt()}% (${CurrencyFormatter.formatPaise(totalExpense, currency)} / ${CurrencyFormatter.formatPaise(overallBudget.monthlyLimit, currency)})"
                )
            } else if (ratio >= 90.0) {
                notificationHelper.notifyBudgetAlert(
                    "Budget Warning (90%)",
                    "Overall monthly spending is at ${ratio.toInt()}% of your limit."
                )
            } else if (ratio >= 75.0) {
                notificationHelper.notifyBudgetAlert(
                    "Budget Warning (75%)",
                    "Overall monthly spending has reached 75%."
                )
            }
        }

        // 2. Category budget check
        if (categoryId != null) {
            val catBudget = budgets.firstOrNull { it.categoryId == categoryId }
            if (catBudget != null && catBudget.monthlyLimit > 0) {
                val catExpense = expenses.filter { it.categoryId == categoryId }.sumOf { it.amount }
                val cat = categoryDao.getCategoryById(categoryId)
                val catName = cat?.name ?: "Category"
                val catRatio = (catExpense.toDouble() / catBudget.monthlyLimit.toDouble()) * 100
                if (catRatio >= 100.0) {
                    notificationHelper.notifyBudgetAlert(
                        "Category Budget Exceeded!",
                        "$catName spending exceeded budget (${catRatio.toInt()}%): ${CurrencyFormatter.formatPaise(catExpense, currency)} / ${CurrencyFormatter.formatPaise(catBudget.monthlyLimit, currency)}"
                    )
                } else if (catRatio >= 90.0) {
                    notificationHelper.notifyBudgetAlert(
                        "Category Budget Warning (90%)",
                        "$catName spending is at ${catRatio.toInt()}% of budget limit."
                    )
                } else if (catRatio >= 75.0) {
                    notificationHelper.notifyBudgetAlert(
                        "Category Budget Alert (75%)",
                        "$catName spending has reached 75% of budget limit."
                    )
                }
            }
        }
    }

    // ==========================================
    // ACCOUNT CREATION & MANAGEMENT
    // ==========================================

    suspend fun createAccount(name: String, type: String, openingBalance: Long): Long = withContext(Dispatchers.IO) {
        val account = AccountEntity(
            name = name,
            type = type,
            openingBalance = openingBalance,
            currentBalance = openingBalance,
            isArchived = false
        )
        val id = accountDao.insertAccount(account)

        // If opening balance > 0, create opening equity ledger entries
        if (openingBalance != 0L) {
            val tx = TransactionEntity(
                type = TransactionType.INCOME.name,
                amount = openingBalance,
                accountId = id,
                toAccountId = null,
                categoryId = null,
                dateEpochDay = LocalDate.now().toEpochDay(),
                timeFormatted = "00:00",
                merchantOrSource = "Opening Balance",
                description = "Initial balance for $name",
                notes = "",
                isVoided = false
            )
            val txId = transactionDao.insertTransaction(tx)
            ledgerDao.insertAll(
                listOf(
                    LedgerEntryEntity(
                        transactionId = txId,
                        accountId = id,
                        accountName = name,
                        debit = openingBalance,
                        credit = 0L,
                        description = "Opening balance debit"
                    ),
                    LedgerEntryEntity(
                        transactionId = txId,
                        accountId = null,
                        accountName = "Opening Equity",
                        debit = 0L,
                        credit = openingBalance,
                        description = "Opening balance credit"
                    )
                )
            )
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                action = "Account created",
                targetId = "ACC#$id",
                details = "Created account '$name' ($type) with initial balance ${CurrencyFormatter.formatPaise(openingBalance)}"
            )
        )
        id
    }

    suspend fun updateAccount(id: Long, name: String, type: String) = withContext(Dispatchers.IO) {
        val acc = accountDao.getAccountById(id) ?: return@withContext
        val updated = acc.copy(name = name, type = type)
        accountDao.updateAccount(updated)
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "Account modified",
                targetId = "ACC#$id",
                details = "Updated account '$name' ($type)"
            )
        )
    }

    suspend fun toggleAccountArchive(id: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        accountDao.setArchived(id, isArchived)
        auditLogDao.insertLog(
            AuditLogEntity(
                action = "Account modified",
                targetId = "ACC#$id",
                details = if (isArchived) "Archived account" else "Unarchived account"
            )
        )
    }

    // ==========================================
    // CATEGORY MANAGEMENT
    // ==========================================

    suspend fun createCategory(name: String, type: String, colorHex: Long): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(
            CategoryEntity(
                name = name,
                type = type,
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteCategory(id: Long) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(id)
    }

    // ==========================================
    // BUDGET MANAGEMENT
    // ==========================================

    suspend fun setBudget(categoryId: Long?, monthlyLimit: Long, month: Int, year: Int) = withContext(Dispatchers.IO) {
        budgetDao.upsertBudget(
            BudgetEntity(
                categoryId = categoryId,
                monthlyLimit = monthlyLimit,
                month = month,
                year = year
            )
        )
    }

    suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudget(id)
    }

    // ==========================================
    // SECURITY & PIN MANAGEMENT
    // ==========================================

    suspend fun verifyPin(enteredPin: String): Boolean = withContext(Dispatchers.IO) {
        val settings = userSettingsDao.getSettings() ?: return@withContext false
        val now = System.currentTimeMillis()

        // Check lockout
        if (now < settings.lockoutUntilMillis) {
            return@withContext false
        }

        val isValid = HashUtils.verifyPin(enteredPin, settings.pinSalt, settings.pinHash)
        if (isValid) {
            // Reset attempts
            userSettingsDao.updateLockout(0, 0L)
            true
        } else {
            val newAttempts = settings.failedPinAttempts + 1
            if (newAttempts >= 3) {
                // Lock access for 30 seconds
                val lockoutUntil = now + 30_000L
                userSettingsDao.updateLockout(0, lockoutUntil)
            } else {
                userSettingsDao.updateLockout(newAttempts, 0L)
            }
            false
        }
    }

    suspend fun changePin(currentPin: String, newPin: String): Boolean = withContext(Dispatchers.IO) {
        val settings = userSettingsDao.getSettings() ?: return@withContext false
        if (!HashUtils.verifyPin(currentPin, settings.pinSalt, settings.pinHash)) {
            return@withContext false
        }

        val newSalt = HashUtils.generateSalt()
        val newHash = HashUtils.hashPin(newPin, newSalt)
        userSettingsDao.updatePin(newHash, newSalt)

        auditLogDao.insertLog(
            AuditLogEntity(
                action = "PIN changed",
                targetId = "SECURITY",
                details = "PIN successfully updated with fresh cryptographic salt and hash."
            )
        )
        true
    }

    suspend fun updateSettings(
        themeMode: String? = null,
        currencySymbol: String? = null,
        biometricEnabled: Boolean? = null,
        notificationsEnabled: Boolean? = null
    ) = withContext(Dispatchers.IO) {
        themeMode?.let { userSettingsDao.updateTheme(it) }
        currencySymbol?.let { userSettingsDao.updateCurrency(it) }
        biometricEnabled?.let { userSettingsDao.updateBiometric(it) }
        notificationsEnabled?.let { userSettingsDao.updateNotifications(it) }
    }

    // ==========================================
    // AUDIT & ACCOUNTING INTEGRITY CHECK
    // ==========================================

    data class IntegrityReport(
        val totalDebits: Long,
        val totalCredits: Long,
        val isLedgerBalanced: Boolean,
        val accountDiscrepancies: List<String>
    ) {
        val isValid: Boolean get() = isLedgerBalanced && accountDiscrepancies.isEmpty()
    }

    suspend fun checkIntegrity(): IntegrityReport = withContext(Dispatchers.IO) {
        val summary = ledgerDao.getLedgerSummary()
        val isBalanced = summary.totalDebit == summary.totalCredit

        // Verify each active account balance against equation:
        // Current balance == openingBalance + Income - Expenses + TransfersIn - TransfersOut
        val allTx = transactionDao.getAllActiveTransactionsSync()
        val accounts = accountDao.getAllAccounts().firstOrNull() ?: emptyList()
        val discrepancies = mutableListOf<String>()

        for (acc in accounts) {
            val accIncome = allTx.filter { it.accountId == acc.id && it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val accExpense = allTx.filter { it.accountId == acc.id && it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val transfersIn = allTx.filter { it.toAccountId == acc.id && it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }
            val transfersOut = allTx.filter { it.accountId == acc.id && it.type == TransactionType.TRANSFER.name }.sumOf { it.amount }

            val expectedBalance = acc.openingBalance + accIncome - accExpense + transfersIn - transfersOut
            if (acc.currentBalance != expectedBalance) {
                discrepancies.add("${acc.name}: Actual=${CurrencyFormatter.formatPaise(acc.currentBalance)}, Calculated=${CurrencyFormatter.formatPaise(expectedBalance)}")
            }
        }

        IntegrityReport(
            totalDebits = summary.totalDebit,
            totalCredits = summary.totalCredit,
            isLedgerBalanced = isBalanced,
            accountDiscrepancies = discrepancies
        )
    }

    // ==========================================
    // CSV EXPORT GENERATOR
    // ==========================================

    suspend fun generateCsvData(): String = withContext(Dispatchers.IO) {
        val transactions = transactionDao.getAllActiveTransactionsSync()
        val accounts = accountDao.getAllAccounts().firstOrNull()?.associateBy { it.id } ?: emptyMap()
        val categories = categoryDao.getAllCategories().firstOrNull()?.associateBy { it.id } ?: emptyMap()

        val sb = StringBuilder()
        sb.append("Date,Time,Type,Category,Account,Amount,Merchant/Source,Description,Notes\n")

        for (tx in transactions) {
            val dateStr = LocalDate.ofEpochDay(tx.dateEpochDay).toString()
            val typeStr = tx.type
            val catName = tx.categoryId?.let { categories[it]?.name } ?: ""
            val accName = accounts[tx.accountId]?.name ?: "Account #${tx.accountId}"
            val amountFormatted = CurrencyFormatter.formatPaise(tx.amount, "")
            val source = tx.merchantOrSource.replace(",", " ")
            val desc = tx.description.replace(",", " ")
            val notes = tx.notes.replace(",", " ")

            sb.append("$dateStr,${tx.timeFormatted},$typeStr,\"$catName\",\"$accName\",$amountFormatted,\"$source\",\"$desc\",\"$notes\"\n")
        }

        sb.toString()
    }
}
