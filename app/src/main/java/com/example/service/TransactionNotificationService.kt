package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.parser.SmsParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TransactionNotificationService {
    private const val CHANNEL_ID = "spend_tracker_channel"
    private const val CHANNEL_NAME = "Spend Tracker Alerts"
    private const val NOTIFICATION_ID = 2001

    fun showAutoSaveNotification(
        context: Context,
        transactionId: String,
        merchant: String,
        amount: Double,
        rawMessage: String,
        time: String?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies after a transaction is successfully auto-saved."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isCredit = SmsParser.isCreditTransaction(rawMessage)
        val title = if (isCredit) "Transaction Auto-Saved (Credit)" else "Transaction Auto-Saved (Debit)"
        val formattedAmount = "₹${String.format(Locale.US, "%,.2f", amount)}"
        val finalTime = time ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        val bodyText = if (isCredit) {
            "Successfully saved: Received $formattedAmount from $merchant at $finalTime"
        } else {
            "Successfully saved: Sent $formattedAmount to $merchant at $finalTime"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .build()

        notificationManager.notify(NOTIFICATION_ID + transactionId.hashCode(), notification)
    }

    fun showNotification(
        context: Context,
        title: String,
        bodyText: String,
        transactionId: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies after a transaction is recorded or updated."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .build()

        notificationManager.notify(NOTIFICATION_ID + transactionId.hashCode() + 100000, notification)
    }
}
