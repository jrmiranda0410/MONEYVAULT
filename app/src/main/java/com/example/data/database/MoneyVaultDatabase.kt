package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import com.example.security.HashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        LedgerEntryEntity::class,
        BudgetEntity::class,
        AuditLogEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MoneyVaultDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun budgetDao(): BudgetDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MoneyVaultDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): MoneyVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneyVaultDatabase::class.java,
                    "moneyvault.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        prepopulateDatabase(database)
                    }
                }
            }
        }

        suspend fun prepopulateDatabase(database: MoneyVaultDatabase) {
            val accountDao = database.accountDao()
            val categoryDao = database.categoryDao()
            val userSettingsDao = database.userSettingsDao()
            val auditLogDao = database.auditLogDao()

            if (accountDao.getCount() == 0) {
                val initialAccounts = listOf(
                    AccountEntity(name = "Bank", type = AccountType.BANK.name, openingBalance = 0L, currentBalance = 0L),
                    AccountEntity(name = "Cash", type = AccountType.CASH.name, openingBalance = 0L, currentBalance = 0L)
                )
                accountDao.insertAll(initialAccounts)
            }

            if (categoryDao.getCount() == 0) {
                val incomeCategories = listOf(
                    "Salary", "Freelance", "Business", "Gift", "Refund", "Interest", "Investment", "Other"
                ).map {
                    CategoryEntity(name = it, type = CategoryType.INCOME.name, colorHex = 0xFF2E7D32)
                }

                val expenseCategories = listOf(
                    "Food", "Groceries", "Transport", "Fuel", "Shopping", "Bills", "Education",
                    "Entertainment", "Healthcare", "Travel", "Rent", "Subscriptions", "Personal", "Other"
                ).map {
                    CategoryEntity(name = it, type = CategoryType.EXPENSE.name, colorHex = 0xFFC62828)
                }

                categoryDao.insertAll(incomeCategories + expenseCategories)
            }

            if (userSettingsDao.getSettings() == null) {
                // Initialize secure salt and hash for initial secret PIN
                val salt = HashUtils.generateSalt()
                val hash = HashUtils.hashPin("0004", salt)
                userSettingsDao.insertOrUpdate(
                    UserSettingsEntity(
                        id = 1,
                        pinHash = hash,
                        pinSalt = salt,
                        biometricEnabled = false,
                        currencySymbol = "₹",
                        themeMode = "SYSTEM",
                        notificationsEnabled = true
                    )
                )
                auditLogDao.insertLog(
                    AuditLogEntity(
                        action = "Vault initialized",
                        targetId = "SYSTEM",
                        details = "Vault created with default Bank and Cash accounts."
                    )
                )
            }
        }
    }
}
