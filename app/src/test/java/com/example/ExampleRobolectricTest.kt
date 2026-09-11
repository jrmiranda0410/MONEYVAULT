package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.accounting.CurrencyFormatter
import com.example.data.database.MoneyVaultDatabase
import com.example.data.repository.MoneyVaultRepository
import com.example.notification.NotificationHelper
import com.example.security.HashUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: MoneyVaultDatabase
    private lateinit var repository: MoneyVaultRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MoneyVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val notificationHelper = NotificationHelper(context)
        repository = MoneyVaultRepository(database, notificationHelper)
    }

    @Test
    fun `read string from context verifies MoneyVault name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("MoneyVault", appName)
    }

    @Test
    fun `currency formatter safely parses decimal to minor units`() {
        val paise = CurrencyFormatter.parseInputToPaise("150.75")
        assertEquals(15075L, paise)
        val formatted = CurrencyFormatter.formatPaise(15075L, "₹")
        assertEquals("₹150.75", formatted)
    }

    @Test
    fun `hash utils generates valid SHA256 and verifies PIN`() {
        val salt = HashUtils.generateSalt()
        val hash = HashUtils.hashPin("1234", salt)
        assertTrue(HashUtils.verifyPin("1234", salt, hash))
        assertFalse(HashUtils.verifyPin("9999", salt, hash))
    }

    @Test
    fun `repository initializes default accounts and categories`() = runBlocking {
        repository.initializeIfEmpty()

        val accounts = repository.accountDao.getAllAccounts().first()
        assertTrue("Expected initial accounts to be created", accounts.isNotEmpty())
        assertTrue("Expected Bank account", accounts.any { it.name == "Bank" })
        assertTrue("Expected Cash account", accounts.any { it.name == "Cash" })

        val categories = repository.categoryDao.getAllCategories().first()
        assertTrue("Expected initial categories", categories.size >= 10)
    }

    @Test
    fun `recording income updates balance and creates double entry ledger`() = runBlocking {
        repository.initializeIfEmpty()
        val bank = repository.accountDao.getAllAccounts().first().first { it.name == "Bank" }
        val initialBalance = bank.currentBalance

        val txId = repository.recordIncome(
            amount = 50000L, // 500.00
            accountId = bank.id,
            categoryId = null,
            dateEpochDay = 1000L,
            timeFormatted = "12:00",
            source = "Test Client",
            description = "Freelance payment",
            notes = ""
        )

        val updatedBank = repository.accountDao.getAccountById(bank.id)
        assertNotNull(updatedBank)
        assertEquals(initialBalance + 50000L, updatedBank!!.currentBalance)

        // Ledger check
        val ledgerEntries = repository.ledgerDao.getEntriesForTransaction(txId).first()
        assertEquals(2, ledgerEntries.size)
        val totalDebit = ledgerEntries.sumOf { it.debit }
        val totalCredit = ledgerEntries.sumOf { it.credit }
        assertEquals(totalDebit, totalCredit)

        // Integrity check
        val report = repository.checkIntegrity()
        assertTrue(report.isLedgerBalanced)
        assertTrue(report.accountDiscrepancies.isEmpty())
    }

    @Test
    fun `voiding transaction reverses balance and preserves history`() = runBlocking {
        repository.initializeIfEmpty()
        val cash = repository.accountDao.getAllAccounts().first().first { it.name == "Cash" }
        val initialBalance = cash.currentBalance

        val txId = repository.recordExpense(
            amount = 2500L, // 25.00
            accountId = cash.id,
            categoryId = null,
            dateEpochDay = 1000L,
            timeFormatted = "13:00",
            merchant = "Coffee",
            description = "Espresso",
            notes = ""
        )

        val afterExpense = repository.accountDao.getAccountById(cash.id)!!
        assertEquals(initialBalance - 2500L, afterExpense.currentBalance)

        // Void the transaction
        repository.voidTransaction(txId)

        val afterVoid = repository.accountDao.getAccountById(cash.id)!!
        assertEquals(initialBalance, afterVoid.currentBalance)

        val tx = repository.transactionDao.getTransactionById(txId)!!
        assertTrue(tx.isVoided)
    }
}
