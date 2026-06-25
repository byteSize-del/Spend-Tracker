package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.remote.SessionManager
import com.example.domain.model.MessageSource
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "transaction_alerts"
        private const val NOTIFICATION_ID = 1002
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        
        // 1. Fast check if Notification Listener is enabled in settings
        val sessionManager = SessionManager(this)
        if (!sessionManager.isNotificationListenerEnabled) {
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        Log.d("NotificationListener", "Intercepted notification from $packageName: $title - $text")

        // 2. Parse transaction details from notification content
        val parsed = parseNotification(title, text) ?: return

        // 3. Save asynchronously
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.transactionDao()
                val currentTime = System.currentTimeMillis()

                // 4. Duplicate prevention:
                // Check if referenceId exists and has already been logged
                if (!parsed.referenceId.isNullOrEmpty()) {
                    val existing = dao.findByReferenceId(parsed.referenceId)
                    if (existing != null) {
                        Log.d("NotificationListener", "Skipping duplicate notification (referenceId match): ${parsed.referenceId}")
                        return@launch
                    }
                }

                // Check for potential duplicate by amount + merchant within a 5-minute window
                val possibleDuplicate = dao.findPotentialDuplicate(
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    currentTime = currentTime,
                    timeWindowMs = 300_000L
                )
                if (possibleDuplicate != null) {
                    Log.d("NotificationListener", "Skipping duplicate notification (amount & merchant match within window): ₹${parsed.amount} at ${parsed.merchant}")
                    return@launch
                }

                // 5. Create transaction entity
                val transactionId = UUID.randomUUID().toString()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                
                val rawMsg = "Notification from $packageName: $title - $text"
                val isNotable = com.example.data.parser.SmsParser.isNotable(rawMsg, packageName)

                val session = sessionManager.getSession()
                val finalTx = Transaction(
                    id = transactionId,
                    userId = session?.userId ?: "local_user",
                    amount = parsed.amount,
                    currency = "INR",
                    merchant = parsed.merchant,
                    category = autoCategorize(parsed.merchant, text),
                    date = today,
                    time = nowTime,
                    referenceId = parsed.referenceId,
                    bankName = parsed.bankName ?: getBankFromPackage(packageName),
                    note = if (isNotable) null else "SYSTEM_UNNOTED",
                    rawMessage = rawMsg,
                    source = MessageSource.NOTIFICATION,
                    isConfirmed = true,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )

                // Save to Room DB
                dao.insertTransaction(TransactionEntity.fromDomain(finalTx, isSynced = false))
                Log.d("NotificationListener", "Successfully auto-saved parsed notification transaction: ${finalTx.merchant} - ₹${finalTx.amount}")

                // Trigger widget update
                DebtTrackWidgetProvider.triggerUpdate(applicationContext)

                // 6. Push user notification alert
                TransactionNotificationService.showAutoSaveNotification(
                    context = applicationContext,
                    transactionId = finalTx.id,
                    merchant = finalTx.merchant ?: "Unknown Merchant",
                    amount = finalTx.amount,
                    rawMessage = rawMsg,
                    time = finalTx.time
                )
            } catch (e: Exception) {
                Log.e("NotificationListener", "Error processing notification", e)
            }
        }
    }

    private fun parseNotification(title: String, text: String): ParsedNotification? {
        val fullText = "$title $text"
        val lowerText = fullText.lowercase()

        // Ignore OTP / Secret verification codes
        val otpKeywords = listOf("otp", "one-time", "one time", "verification", "do not share", "secret")
        for (otp in otpKeywords) {
            if (lowerText.contains(otp)) return null
        }

        // Must look like a debit/transaction alert
        val debitKeywords = listOf("debited", "spent", "paid", "charged", "deducted", "withdrawn", "sent to", "transferred to", "transfer to")
        var isDebit = false
        for (debit in debitKeywords) {
            if (lowerText.contains(debit)) {
                isDebit = true
                break
            }
        }
        if (!isDebit) return null

        // Amount parsing
        val amountRegex = Regex("(?:Rs\\.?|INR|₹)\\s?(\\d+[,\\d]*\\.?\\d*)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(fullText) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // Recipient / Merchant Parsing
        var merchant = "Unknown Merchant"
        val merchantPatterns = listOf(
            Regex("(?:paid|sent|transferred|transfer|spent|charged)\\s+(?:Rs\\.?|INR|₹)\\s?\\d+(?:\\.\\d+)?\\s+(?:to|at|towards)\\s+([A-Za-z0-9\\s\\-&.]+)", RegexOption.IGNORE_CASE),
            Regex("(?:at|to|towards|vpa)\\s+([A-Z][A-Za-z0-9\\s\\-&.]+)", RegexOption.IGNORE_CASE),
            Regex("(?:debited\\s+for|spent\\s+at)\\s+([A-Za-z0-9\\s\\-&.]+)", RegexOption.IGNORE_CASE)
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty() && !candidate.equals("to", true) && !candidate.equals("at", true)) {
                    merchant = candidate
                    break
                }
            }
        }

        if (merchant.endsWith(".") || merchant.endsWith(",")) {
            merchant = merchant.substring(0, merchant.length - 1).trim()
        }

        // Reference ID
        val refRegex = Regex("(?:Ref\\.?No\\.?|TXN|UPI Ref|Ref)\\s*[:\\-]?\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE)
        val refMatch = refRegex.find(fullText)
        val referenceId = refMatch?.groupValues[1]?.trim()

        // Bank Name
        var bankName: String? = null
        val bankRegex = Regex("\\b(SBI|HDFC|ICICI|Axis|Kotak|IDBI|PNB|BOI|Union|Paytm|PhonePe|GPay)\\b", RegexOption.IGNORE_CASE)
        val bankMatch = bankRegex.find(fullText)
        if (bankMatch != null) {
            bankName = bankMatch.groupValues[0].uppercase()
        }

        return ParsedNotification(amount, merchant, referenceId, bankName)
    }

    private fun getBankFromPackage(packageName: String): String? {
        return when {
            packageName.contains("paytm", true) -> "PAYTM"
            packageName.contains("phonepe", true) -> "PHONEPE"
            packageName.contains("nbu.paisa", true) -> "GPAY"
            else -> null
        }
    }

    private fun autoCategorize(merchant: String, body: String): TransactionCategory {
        val text = "$merchant $body".lowercase()
        return when {
            text.contains("swiggy") || text.contains("zomato") || text.contains("restaurant") || text.contains("food") || text.contains("cafe") || text.contains("hotel") || text.contains("diner") || text.contains("eats") -> TransactionCategory.FOOD
            text.contains("uber") || text.contains("ola") || text.contains("rapido") || text.contains("metro") || text.contains("cab") || text.contains("train") || text.contains("flight") || text.contains("bus") || text.contains("fuel") || text.contains("petrol") -> TransactionCategory.TRANSPORT
            text.contains("amazon") || text.contains("flipkart") || text.contains("myntra") || text.contains("mall") || text.contains("shopping") || text.contains("apparel") || text.contains("clothing") || text.contains("mart") || text.contains("supermarket") || text.contains("groceries") || text.contains("blinkit") || text.contains("zepto") || text.contains("instamart") -> TransactionCategory.SHOPPING
            text.contains("electricity") || text.contains("water") || text.contains("gas") || text.contains("broadband") || text.contains("recharge") || text.contains("bill") || text.contains("jio") || text.contains("airtel") || text.contains("vi ") -> TransactionCategory.UTILITIES
            text.contains("netflix") || text.contains("prime") || text.contains("spotify") || text.contains("theatre") || text.contains("cinema") || text.contains("movie") || text.contains("show") || text.contains("ticket") || text.contains("game") -> TransactionCategory.ENTERTAINMENT
            text.contains("hospital") || text.contains("pharmacy") || text.contains("medical") || text.contains("doctor") || text.contains("clinic") || text.contains("health") || text.contains("medicine") -> TransactionCategory.HEALTH
            text.contains("school") || text.contains("college") || text.contains("fees") || text.contains("course") || text.contains("book") || text.contains("udemy") || text.contains("coursera") -> TransactionCategory.EDUCATION
            text.contains("rent") || text.contains("pg ") || text.contains("room rent") || text.contains("landlord") -> TransactionCategory.RENT
            text.contains("transfer") || text.contains("sent to") || text.contains("transfer to") -> TransactionCategory.TRANSFER
            else -> TransactionCategory.OTHER
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional hook
    }

    data class ParsedNotification(
        val amount: Double,
        val merchant: String,
        val referenceId: String?,
        val bankName: String?
    )
}
