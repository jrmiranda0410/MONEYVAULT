package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.UserSettingsEntity
import com.example.data.repository.MoneyVaultRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userSettings: UserSettingsEntity?,
    auditLogs: List<AuditLogEntity>,
    integrityReport: MoneyVaultRepository.IntegrityReport?,
    onUpdateTheme: (String) -> Unit,
    onUpdateCurrency: (String) -> Unit,
    onToggleBiometrics: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onChangePin: (oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onRunIntegrityCheck: () -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onLockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showIntegrityDialog by remember { mutableStateOf(false) }

    val currentTheme = userSettings?.themeMode ?: "SYSTEM"
    val currentCurrency = userSettings?.currencySymbol ?: "₹"
    val biometricsEnabled = userSettings?.biometricEnabled ?: false
    val notificationsEnabled = userSettings?.notificationsEnabled ?: true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Vault", fontWeight = FontWeight.Bold) }
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
            // Security Section
            item {
                Text(
                    text = "SECURITY & ACCESS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Pin,
                            title = "Change 4-Digit Passcode",
                            subtitle = "Update your private vault lock PIN",
                            onClick = { showChangePinDialog = true },
                            testTag = "setting_change_pin"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Biometric Authentication", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Unlock with fingerprint / face", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Switch(
                                checked = biometricsEnabled,
                                onCheckedChange = onToggleBiometrics,
                                modifier = Modifier.testTag("switch_biometrics")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsRow(
                            icon = Icons.Default.Lock,
                            title = "Lock MoneyVault Now",
                            subtitle = "Immediately secure the application",
                            onClick = onLockApp,
                            testTag = "setting_lock_now"
                        )
                    }
                }
            }

            // Preferences Section
            item {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.CurrencyExchange,
                            title = "Currency Symbol",
                            subtitle = "Currently: $currentCurrency",
                            onClick = { showCurrencyDialog = true },
                            testTag = "setting_currency"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsRow(
                            icon = Icons.Default.DarkMode,
                            title = "App Theme",
                            subtitle = when (currentTheme) {
                                "LIGHT" -> "Light Mode"
                                "DARK" -> "Dark Mode"
                                else -> "Follow System"
                            },
                            onClick = { showThemeDialog = true },
                            testTag = "setting_theme"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Budget & System Notifications", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Receive alerts when spending nears limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = onToggleNotifications,
                                modifier = Modifier.testTag("switch_notifications")
                            )
                        }
                    }
                }
            }

            // Accounting & Audit Section
            item {
                Text(
                    text = "ACCOUNTING & AUDIT",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.VerifiedUser,
                            title = "Double-Entry Integrity Audit",
                            subtitle = "Verify Debits = Credits and Ledger Consistency",
                            onClick = {
                                onRunIntegrityCheck()
                                showIntegrityDialog = true
                            },
                            testTag = "setting_integrity_check"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsRow(
                            icon = Icons.Default.History,
                            title = "View Security Audit Log",
                            subtitle = "Recent events: ${auditLogs.size} logs recorded",
                            onClick = { showAuditLogsDialog = true },
                            testTag = "setting_audit_log"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        SettingsRow(
                            icon = Icons.Default.FileDownload,
                            title = "Export Data (CSV Format)",
                            subtitle = "Export complete records to CSV share sheet",
                            onClick = {
                                onExportCsv { csvContent ->
                                    shareCsv(context, csvContent)
                                }
                            },
                            testTag = "setting_export_csv"
                        )
                    }
                }
            }

            // App Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("MoneyVault v1.0.0", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Private personal finance app for single-user native Android device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("All financial data stored locally with cryptographic PIN hashing and immutable double-entry ledger.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onChangePin = onChangePin
        )
    }

    // Currency Selector Dialog
    if (showCurrencyDialog) {
        val currencies = listOf("₹" to "INR - Indian Rupee", "$" to "USD - US Dollar", "€" to "EUR - Euro", "£" to "GBP - British Pound", "¥" to "JPY - Japanese Yen", "A$" to "AUD - Australian Dollar", "C$" to "CAD - Canadian Dollar", "AED" to "AED - Dirham")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency Symbol") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currencies.forEach { (sym, name) ->
                        TextButton(
                            onClick = {
                                onUpdateCurrency(sym)
                                showCurrencyDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$sym  •  $name", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    // Theme Mode Dialog
    if (showThemeDialog) {
        val themes = listOf("SYSTEM" to "Follow System", "LIGHT" to "Light Mode", "DARK" to "Dark Mode")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    themes.forEach { (mode, label) ->
                        TextButton(
                            onClick = {
                                onUpdateTheme(mode)
                                showThemeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    // Integrity Audit Report Dialog
    if (showIntegrityDialog) {
        AlertDialog(
            onDismissRequest = { showIntegrityDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (integrityReport?.isValid == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (integrityReport?.isValid == true) EmeraldPrimary else ExpenseRed
                    )
                    Text("Ledger Integrity Status")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (integrityReport != null) {
                        Text(
                            text = if (integrityReport.isValid) "Double-entry integrity check passed. Total Debits exactly equal Total Credits."
                            else "Discrepancy detected in ledger validation.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Total Debits: ₹${String.format("%.2f", integrityReport.totalDebits / 100.0)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Total Credits: ₹${String.format("%.2f", integrityReport.totalCredits / 100.0)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showIntegrityDialog = false }) { Text("OK") }
            }
        )
    }

    // Audit Logs Viewer Dialog
    if (showAuditLogsDialog) {
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            title = { Text("Audit Trail") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(auditLogs) { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.action, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Text(dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditLogsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onChangePin: (oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Passcode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPin = it },
                    label = { Text("Current 4-Digit Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_current_pin")
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("New 4-Digit Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_new_pin")
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm New Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_confirm_pin")
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                }

                if (successMessage != null) {
                    Text(successMessage!!, color = EmeraldPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPin.length != 4) {
                        errorMessage = "Please enter your 4-digit current passcode."
                        return@Button
                    }
                    if (newPin.length != 4) {
                        errorMessage = "New passcode must be exactly 4 digits."
                        return@Button
                    }
                    if (newPin != confirmPin) {
                        errorMessage = "New passcodes do not match."
                        return@Button
                    }
                    onChangePin(oldPin, newPin) { success, msg ->
                        if (success) {
                            successMessage = msg
                            errorMessage = null
                        } else {
                            errorMessage = msg
                            successMessage = null
                        }
                    }
                },
                modifier = Modifier.testTag("btn_confirm_change_pin")
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun shareCsv(context: Context, csvContent: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvContent)
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export MoneyVault CSV")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        // Fallback to text/plain
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvContent)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export MoneyVault CSV")
        context.startActivity(shareIntent)
    }
}
