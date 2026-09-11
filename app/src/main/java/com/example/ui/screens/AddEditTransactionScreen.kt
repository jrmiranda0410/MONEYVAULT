package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.accounting.CurrencyFormatter
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.CategoryType
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    initialType: String, // INCOME, EXPENSE, TRANSFER
    existingTransaction: TransactionEntity? = null,
    isDuplicate: Boolean = false,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onSaveIncome: (amount: Long, accountId: Long, categoryId: Long?, dateEpoch: Long, timeStr: String, source: String, desc: String, notes: String) -> Unit,
    onSaveExpense: (amount: Long, accountId: Long, categoryId: Long?, dateEpoch: Long, timeStr: String, merchant: String, desc: String, notes: String) -> Unit,
    onSaveTransfer: (amount: Long, fromAccountId: Long, toAccountId: Long, dateEpoch: Long, timeStr: String, desc: String, notes: String) -> Unit,
    onSaveEdit: (txId: Long, amount: Long, accountId: Long, toAccountId: Long?, categoryId: Long?, dateEpoch: Long, timeStr: String, merchantOrSource: String, desc: String, notes: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEditMode = existingTransaction != null && !isDuplicate

    var selectedType by remember {
        mutableStateOf(existingTransaction?.type ?: initialType)
    }

    var amountInput by remember {
        mutableStateOf(
            if (existingTransaction != null) CurrencyFormatter.paiseToInputString(existingTransaction.amount)
            else ""
        )
    }

    var selectedAccountId by remember {
        mutableStateOf(
            existingTransaction?.accountId ?: accounts.firstOrNull()?.id ?: 0L
        )
    }

    var selectedToAccountId by remember {
        mutableStateOf(
            existingTransaction?.toAccountId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 0L
        )
    }

    var selectedCategoryId by remember {
        mutableStateOf<Long?>(existingTransaction?.categoryId)
    }

    var merchantOrSourceInput by remember {
        mutableStateOf(existingTransaction?.merchantOrSource ?: "")
    }

    var descriptionInput by remember {
        mutableStateOf(existingTransaction?.description ?: "")
    }

    var notesInput by remember {
        mutableStateOf(existingTransaction?.notes ?: "")
    }

    var selectedDate by remember {
        mutableStateOf(
            if (isDuplicate || existingTransaction == null) LocalDate.now()
            else LocalDate.ofEpochDay(existingTransaction.dateEpochDay)
        )
    }

    var selectedTime by remember {
        mutableStateOf(
            if (isDuplicate || existingTransaction == null) LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            else existingTransaction.timeFormatted
        )
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Filtered categories for type
    val relevantCategories = remember(selectedType, categories) {
        if (selectedType == TransactionType.INCOME.name) {
            categories.filter { it.type == CategoryType.INCOME.name }
        } else if (selectedType == TransactionType.EXPENSE.name) {
            categories.filter { it.type == CategoryType.EXPENSE.name }
        } else {
            emptyList()
        }
    }

    // Auto-select category if none selected
    LaunchedEffect(relevantCategories) {
        if (selectedCategoryId == null && relevantCategories.isNotEmpty() && selectedType != TransactionType.TRANSFER.name) {
            selectedCategoryId = relevantCategories.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isEditMode -> "Edit Transaction"
                            isDuplicate -> "Duplicate Transaction"
                            selectedType == TransactionType.INCOME.name -> "Record Income"
                            selectedType == TransactionType.EXPENSE.name -> "Record Expense"
                            else -> "Record Transfer"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector Tabs (Only if creating new, or duplicating)
            if (!isEditMode) {
                TabRow(
                    selectedTabIndex = when (selectedType) {
                        TransactionType.INCOME.name -> 0
                        TransactionType.EXPENSE.name -> 1
                        else -> 2
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedType == TransactionType.INCOME.name,
                        onClick = { selectedType = TransactionType.INCOME.name },
                        text = { Text("+ Income", fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = IncomeGreen
                    )
                    Tab(
                        selected = selectedType == TransactionType.EXPENSE.name,
                        onClick = { selectedType = TransactionType.EXPENSE.name },
                        text = { Text("- Expense", fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = ExpenseRed
                    )
                    Tab(
                        selected = selectedType == TransactionType.TRANSFER.name,
                        onClick = { selectedType = TransactionType.TRANSFER.name },
                        text = { Text("Transfer", fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = TransferBlue
                    )
                }
            }

            // Amount Input Field
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    if (input.count { it == '.' } <= 1 && input.all { it.isDigit() || it == '.' || it == ',' }) {
                        amountInput = input
                        validationError = null
                    }
                },
                label = { Text("Amount ($currencySymbol)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_amount"),
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                isError = validationError != null && validationError!!.contains("amount", ignoreCase = true)
            )

            // Category Selection (for Income / Expense)
            if (selectedType != TransactionType.TRANSFER.name) {
                var categoryExpanded by remember { mutableStateOf(false) }
                val currentCategory = categories.firstOrNull { it.id == selectedCategoryId }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("dropdown_category")
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        relevantCategories.forEach { cat ->
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

            // Merchant or Source field
            if (selectedType == TransactionType.INCOME.name) {
                OutlinedTextField(
                    value = merchantOrSourceInput,
                    onValueChange = { merchantOrSourceInput = it },
                    label = { Text("Source (e.g. Employer, Client, Investment)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_source")
                )
            } else if (selectedType == TransactionType.EXPENSE.name) {
                OutlinedTextField(
                    value = merchantOrSourceInput,
                    onValueChange = { merchantOrSourceInput = it },
                    label = { Text("Merchant / Payee (e.g. Supermarket, Shell)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_merchant")
                )
            }

            // Account Selectors
            if (selectedType == TransactionType.TRANSFER.name) {
                // From Account
                var fromExpanded by remember { mutableStateOf(false) }
                val fromAccount = accounts.firstOrNull { it.id == selectedAccountId }

                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = fromAccount?.let { "${it.name} (${CurrencyFormatter.formatPaise(it.currentBalance, currencySymbol)})" } ?: "Select Source Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("dropdown_from_account")
                    )

                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.type}) - ${CurrencyFormatter.formatPaise(acc.currentBalance, currencySymbol)}") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                // To Account
                var toExpanded by remember { mutableStateOf(false) }
                val toAccount = accounts.firstOrNull { it.id == selectedToAccountId }

                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = toAccount?.let { "${it.name} (${CurrencyFormatter.formatPaise(it.currentBalance, currencySymbol)})" } ?: "Select Destination Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("dropdown_to_account"),
                        isError = selectedAccountId == selectedToAccountId
                    )

                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.type}) - ${CurrencyFormatter.formatPaise(acc.currentBalance, currencySymbol)}") },
                                onClick = {
                                    selectedToAccountId = acc.id
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedAccountId == selectedToAccountId) {
                    Text(
                        text = "Source and destination accounts must be different.",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                // Single Account Selector (for Income or Expense)
                var accountExpanded by remember { mutableStateOf(false) }
                val account = accounts.firstOrNull { it.id == selectedAccountId }

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = account?.let { "${it.name} (${CurrencyFormatter.formatPaise(it.currentBalance, currencySymbol)})" } ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (selectedType == TransactionType.INCOME.name) "Deposit To Account" else "Pay From Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("dropdown_account")
                    )

                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.type}) - ${CurrencyFormatter.formatPaise(acc.currentBalance, currencySymbol)}") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Date & Time Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Picker
                OutlinedTextField(
                    value = selectedDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            ).show()
                        }
                        .testTag("picker_date"),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Time Picker
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time") },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = "Pick Time")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val timeParts = selectedTime.split(":")
                            val hr = timeParts.getOrNull(0)?.toIntOrNull() ?: 12
                            val min = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                                },
                                hr,
                                min,
                                true
                            ).show()
                        }
                        .testTag("picker_time"),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Description
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Description") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_description")
            )

            // Notes
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Notes (Optional)") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_notes")
            )

            // Validation error display
            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val paise = CurrencyFormatter.parseInputToPaise(amountInput)
                    if (paise == null || paise <= 0) {
                        validationError = "Please enter a valid positive amount."
                        return@Button
                    }
                    if (selectedAccountId == 0L) {
                        validationError = "Please select an account."
                        return@Button
                    }
                    if (selectedType == TransactionType.TRANSFER.name && selectedAccountId == selectedToAccountId) {
                        validationError = "Source and destination accounts must be different."
                        return@Button
                    }

                    if (isEditMode) {
                        onSaveEdit(
                            existingTransaction.id,
                            paise,
                            selectedAccountId,
                            if (selectedType == TransactionType.TRANSFER.name) selectedToAccountId else null,
                            if (selectedType == TransactionType.TRANSFER.name) null else selectedCategoryId,
                            selectedDate.toEpochDay(),
                            selectedTime,
                            merchantOrSourceInput,
                            descriptionInput,
                            notesInput
                        )
                    } else {
                        when (selectedType) {
                            TransactionType.INCOME.name -> {
                                onSaveIncome(
                                    paise,
                                    selectedAccountId,
                                    selectedCategoryId,
                                    selectedDate.toEpochDay(),
                                    selectedTime,
                                    merchantOrSourceInput,
                                    descriptionInput,
                                    notesInput
                                )
                            }
                            TransactionType.EXPENSE.name -> {
                                onSaveExpense(
                                    paise,
                                    selectedAccountId,
                                    selectedCategoryId,
                                    selectedDate.toEpochDay(),
                                    selectedTime,
                                    merchantOrSourceInput,
                                    descriptionInput,
                                    notesInput
                                )
                            }
                            TransactionType.TRANSFER.name -> {
                                onSaveTransfer(
                                    paise,
                                    selectedAccountId,
                                    selectedToAccountId,
                                    selectedDate.toEpochDay(),
                                    selectedTime,
                                    descriptionInput,
                                    notesInput
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_transaction"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        TransactionType.INCOME.name -> IncomeGreen
                        TransactionType.EXPENSE.name -> ExpenseRed
                        else -> TransferBlue
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Save Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
