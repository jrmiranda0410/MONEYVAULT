package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accounting.CurrencyFormatter
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: Long,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onEditClick: (Long) -> Unit,
    onDuplicateClick: (Long) -> Unit,
    onVoidClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transaction = transactions.firstOrNull { it.id == transactionId }
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    var showVoidDialog by remember { mutableStateOf(false) }

    val dateTimeFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Transaction not found.")
            }
            return@Scaffold
        }

        val account = accountsMap[transaction.accountId]
        val toAccount = transaction.toAccountId?.let { accountsMap[it] }
        val category = transaction.categoryId?.let { categoriesMap[it] }

        val typeColor = when (transaction.type) {
            TransactionType.INCOME.name -> IncomeGreen
            TransactionType.EXPENSE.name -> ExpenseRed
            else -> TransferBlue
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Amount Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (transaction.isVoided) {
                        Surface(
                            color = ExpenseRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "VOIDED",
                                color = ExpenseRed,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = when (transaction.type) {
                            TransactionType.INCOME.name -> "+${CurrencyFormatter.formatPaise(transaction.amount, currencySymbol)}"
                            TransactionType.EXPENSE.name -> "-${CurrencyFormatter.formatPaise(transaction.amount, currencySymbol)}"
                            else -> CurrencyFormatter.formatPaise(transaction.amount, currencySymbol)
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (transaction.isVoided) VoidGray else typeColor
                    )

                    Text(
                        text = transaction.type,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Details Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow("Transaction ID", "TX#${transaction.id}")

                    if (transaction.type == TransactionType.TRANSFER.name) {
                        DetailRow("From Account", account?.name ?: "Account #${transaction.accountId}")
                        DetailRow("To Account", toAccount?.name ?: "Account #${transaction.toAccountId}")
                    } else {
                        DetailRow("Account", account?.name ?: "Account #${transaction.accountId}")
                        if (category != null) {
                            DetailRow("Category", category.name)
                        }
                        if (transaction.merchantOrSource.isNotBlank()) {
                            DetailRow(
                                if (transaction.type == TransactionType.INCOME.name) "Source" else "Merchant",
                                transaction.merchantOrSource
                            )
                        }
                    }

                    val dateText = try {
                        val ld = LocalDate.ofEpochDay(transaction.dateEpochDay)
                        "${ld.dayOfMonth} ${ld.month.name.lowercase().replaceFirstChar { it.titlecase() }}, ${ld.year}"
                    } catch (e: Exception) {
                        "${transaction.dateEpochDay}"
                    }
                    DetailRow("Date", dateText)
                    DetailRow("Time", transaction.timeFormatted)

                    if (transaction.description.isNotBlank()) {
                        DetailRow("Description", transaction.description)
                    }

                    if (transaction.notes.isNotBlank()) {
                        DetailRow("Notes", transaction.notes)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    DetailRow("Created Time", dateTimeFormat.format(Date(transaction.createdAt)))
                    DetailRow("Updated Time", dateTimeFormat.format(Date(transaction.updatedAt)))
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Edit Button (Only available if NOT voided)
                if (!transaction.isVoided) {
                    Button(
                        onClick = { onEditClick(transaction.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_edit_tx"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Transaction")
                    }
                }

                // Duplicate Button
                OutlinedButton(
                    onClick = { onDuplicateClick(transaction.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_duplicate_tx"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duplicate Transaction")
                }

                // Void / Delete Button (Only if NOT already voided)
                if (!transaction.isVoided) {
                    OutlinedButton(
                        onClick = { showVoidDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_void_tx"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ExpenseRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Void Transaction")
                    }
                }
            }
        }
    }

    if (showVoidDialog && transaction != null) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            title = { Text("Void Transaction?") },
            text = {
                Text(
                    "Voiding will reverse its accounting impact and balance changes. The transaction will remain permanently in your history marked as VOIDED."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVoidDialog = false
                        onVoidClick(transaction.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Confirm Void")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}
