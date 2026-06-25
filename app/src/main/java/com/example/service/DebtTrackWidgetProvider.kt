package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebtTrackWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.service.ACTION_UPDATE_WIDGET"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, DebtTrackWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgetData(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, DebtTrackWidgetProvider::class.java)
            )
            updateWidgetData(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidgetData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        coroutineScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.transactionDao()

                // Fetch transactions from background thread
                val transactions = withContext(Dispatchers.IO) {
                    dao.getAllTransactions()
                }

                // Filter out SYSTEM_UNNOTED transactions (just like the app's dashboard)
                val visibleTransactions = transactions.filter { it.note != "SYSTEM_UNNOTED" }

                // Calculate Monthly Total of confirmed transactions
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val monthlyTotal = visibleTransactions
                    .filter { it.isConfirmed && it.date.startsWith(currentMonthStr) }
                    .sumOf { it.amount }

                val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                    maximumFractionDigits = 2
                }
                val formattedMonthlySpend = currencyFormatter.format(monthlyTotal)

                // Get top 3 visible transactions
                val recentTransactions = visibleTransactions.take(3)

                // Count unconfirmed transactions for the badge
                val unconfirmedCount = visibleTransactions.count { !it.isConfirmed }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.debt_track_widget)

                    // Update monthly total
                    views.setTextViewText(R.id.widget_monthly_total, formattedMonthlySpend)

                    // Update pending badge
                    if (unconfirmedCount > 0) {
                        views.setViewVisibility(R.id.widget_pending_badge, View.VISIBLE)
                        views.setTextViewText(R.id.widget_pending_badge, "$unconfirmedCount pending")
                    } else {
                        views.setViewVisibility(R.id.widget_pending_badge, View.GONE)
                    }

                    if (recentTransactions.isEmpty()) {
                        views.setViewVisibility(R.id.widget_tx_list_container, View.GONE)
                        views.setViewVisibility(R.id.widget_no_tx_text, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.widget_tx_list_container, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_no_tx_text, View.GONE)

                        // Bind Row 1
                        if (recentTransactions.size >= 1) {
                            val tx = recentTransactions[0]
                            views.setViewVisibility(R.id.widget_tx1_container, View.VISIBLE)
                            
                            val emoji = getCategoryEmoji(tx.category)
                            val merchantText = if (tx.isConfirmed) {
                                "$emoji ${tx.merchant ?: "Unknown Payee"}"
                            } else {
                                "⚠️ $emoji ${tx.merchant ?: "Unknown Payee"}"
                            }
                            views.setTextViewText(R.id.widget_tx1_merchant, merchantText)
                            
                            val categoryFormatted = tx.category.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
                            val metaText = if (tx.isConfirmed) {
                                "$categoryFormatted • ${tx.date}"
                            } else {
                                "$categoryFormatted • ${tx.date} • REVIEW"
                            }
                            views.setTextViewText(R.id.widget_tx1_meta, metaText)
                            
                            views.setTextViewText(R.id.widget_tx1_amount, "-${currencyFormatter.format(tx.amount)}")
                            
                            if (tx.isConfirmed) {
                                views.setTextColor(R.id.widget_tx1_amount, android.graphics.Color.WHITE)
                            } else {
                                views.setTextColor(R.id.widget_tx1_amount, android.graphics.Color.parseColor("#E0E0E0"))
                            }
                        } else {
                            views.setViewVisibility(R.id.widget_tx1_container, View.GONE)
                        }

                        // Bind Row 2
                        if (recentTransactions.size >= 2) {
                            val tx = recentTransactions[1]
                            views.setViewVisibility(R.id.widget_tx2_container, View.VISIBLE)
                            
                            val emoji = getCategoryEmoji(tx.category)
                            val merchantText = if (tx.isConfirmed) {
                                "$emoji ${tx.merchant ?: "Unknown Payee"}"
                            } else {
                                "⚠️ $emoji ${tx.merchant ?: "Unknown Payee"}"
                            }
                            views.setTextViewText(R.id.widget_tx2_merchant, merchantText)
                            
                            val categoryFormatted = tx.category.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
                            val metaText = if (tx.isConfirmed) {
                                "$categoryFormatted • ${tx.date}"
                            } else {
                                "$categoryFormatted • ${tx.date} • REVIEW"
                            }
                            views.setTextViewText(R.id.widget_tx2_meta, metaText)
                            
                            views.setTextViewText(R.id.widget_tx2_amount, "-${currencyFormatter.format(tx.amount)}")
                            
                            if (tx.isConfirmed) {
                                views.setTextColor(R.id.widget_tx2_amount, android.graphics.Color.WHITE)
                            } else {
                                views.setTextColor(R.id.widget_tx2_amount, android.graphics.Color.parseColor("#E0E0E0"))
                            }
                        } else {
                            views.setViewVisibility(R.id.widget_tx2_container, View.GONE)
                        }

                        // Bind Row 3
                        if (recentTransactions.size >= 3) {
                            val tx = recentTransactions[2]
                            views.setViewVisibility(R.id.widget_tx3_container, View.VISIBLE)
                            
                            val emoji = getCategoryEmoji(tx.category)
                            val merchantText = if (tx.isConfirmed) {
                                "$emoji ${tx.merchant ?: "Unknown Payee"}"
                            } else {
                                "⚠️ $emoji ${tx.merchant ?: "Unknown Payee"}"
                            }
                            views.setTextViewText(R.id.widget_tx3_merchant, merchantText)
                            
                            val categoryFormatted = tx.category.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
                            val metaText = if (tx.isConfirmed) {
                                "$categoryFormatted • ${tx.date}"
                            } else {
                                "$categoryFormatted • ${tx.date} • REVIEW"
                            }
                            views.setTextViewText(R.id.widget_tx3_meta, metaText)
                            
                            views.setTextViewText(R.id.widget_tx3_amount, "-${currencyFormatter.format(tx.amount)}")
                            
                            if (tx.isConfirmed) {
                                views.setTextColor(R.id.widget_tx3_amount, android.graphics.Color.WHITE)
                            } else {
                                views.setTextColor(R.id.widget_tx3_amount, android.graphics.Color.parseColor("#E0E0E0"))
                            }
                        } else {
                            views.setViewVisibility(R.id.widget_tx3_container, View.GONE)
                        }
                    }

                    // Setup click intent to launch Main App
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntentFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        appIntent,
                        pendingIntentFlags
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_open, pendingIntent)

                    // Apply update to the widget manager
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("DebtTrackWidget", "Error updating widget data", e)
            }
        }
    }

    private fun getCategoryEmoji(category: String): String {
        return when (category.uppercase(Locale.getDefault())) {
            "FOOD" -> "🍔"
            "TRANSPORT" -> "🚗"
            "SHOPPING" -> "🛍️"
            "UTILITIES" -> "⚡"
            "ENTERTAINMENT" -> "🎬"
            "HEALTH" -> "🏥"
            "EDUCATION" -> "🎓"
            "RENT" -> "🏠"
            "TRANSFER" -> "💸"
            else -> "📝"
        }
    }
}
