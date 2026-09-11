package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_TRANSACTIONS = "moneyvault_transactions"
        const val CHANNEL_BUDGETS = "moneyvault_budgets"
        private var notificationId = 1000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val txChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Financial Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for recorded income, expenses, and transfers."
            }

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGETS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings and alerts when approaching or exceeding budgets."
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(txChannel)
            notificationManager?.createNotificationChannel(budgetChannel)
        }
    }

    fun notifyTransactionRecorded(title: String, message: String) {
        sendNotification(CHANNEL_TRANSACTIONS, title, message)
    }

    fun notifyBudgetAlert(title: String, message: String) {
        sendNotification(CHANNEL_BUDGETS, title, message)
    }

    private fun sendNotification(channelId: String, title: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(
                    if (channelId == CHANNEL_BUDGETS) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId++, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted or restricted
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
