package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accounting.CurrencyFormatter
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionType
import com.example.ui.theme.*
import java.time.LocalDate

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    accountsMap: Map<Long, AccountEntity>,
    categoriesMap: Map<Long, CategoryEntity>,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVoided = transaction.isVoided
    val account = accountsMap[transaction.accountId]
    val toAccount = transaction.toAccountId?.let { accountsMap[it] }
    val category = transaction.categoryId?.let { categoriesMap[it] }

    val (typeColor, typeIcon, typePrefix) = when (transaction.type) {
        TransactionType.INCOME.name -> Triple(IncomeGreen, Icons.AutoMirrored.Filled.CallReceived, "+")
        TransactionType.EXPENSE.name -> Triple(ExpenseRed, Icons.AutoMirrored.Filled.CallMade, "-")
        else -> Triple(TransferBlue, Icons.AutoMirrored.Filled.CompareArrows, "")
    }

    val primaryTitle = when {
        transaction.type == TransactionType.TRANSFER.name -> {
            "${account?.name ?: "Account"} → ${toAccount?.name ?: "Account"}"
        }
        category != null -> category.name
        transaction.merchantOrSource.isNotBlank() -> transaction.merchantOrSource
        transaction.description.isNotBlank() -> transaction.description
        else -> transaction.type
    }

    val subtitle = buildString {
        if (transaction.type != TransactionType.TRANSFER.name && account != null) {
            append(account.name)
        }
        if (transaction.merchantOrSource.isNotBlank() && category != null) {
            if (isNotEmpty()) append(" • ")
            append(transaction.merchantOrSource)
        }
        if (transaction.description.isNotBlank() && primaryTitle != transaction.description) {
            if (isNotEmpty()) append(" • ")
            append(transaction.description)
        }
    }

    val dateStr = try {
        val ld = LocalDate.ofEpochDay(transaction.dateEpochDay)
        "${ld.dayOfMonth} ${ld.month.name.take(3)}, ${transaction.timeFormatted}"
    } catch (e: Exception) {
        transaction.timeFormatted
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("tx_item_${transaction.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isVoided) VoidGray.copy(alpha = 0.2f) else typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = transaction.type,
                    tint = if (isVoided) VoidGray else typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = primaryTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isVoided) VoidGray else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isVoided) {
                        Surface(
                            color = ExpenseRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "VOIDED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = ExpenseRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (transaction.type == TransactionType.TRANSFER.name) {
                        CurrencyFormatter.formatPaise(transaction.amount, currencySymbol)
                    } else {
                        "$typePrefix${CurrencyFormatter.formatPaise(transaction.amount, currencySymbol)}"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = when {
                        isVoided -> VoidGray
                        transaction.type == TransactionType.INCOME.name -> IncomeGreen
                        transaction.type == TransactionType.EXPENSE.name -> ExpenseRed
                        else -> TransferBlue
                    }
                )
            }
        }
    }
}
