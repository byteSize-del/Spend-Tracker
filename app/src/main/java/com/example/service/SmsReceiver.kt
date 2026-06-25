package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.parser.SmsParser
import com.example.data.remote.SessionManager
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "transaction_alerts"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val sessionManager = SessionManager(context)
        if (!sessionManager.isSmsReadEnabled) {
            Log.d("SmsReceiver", "SMS reading is disabled in settings")
            return
        }

        val bundle = intent.extras ?: return
        try {
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")
            
            for (pdu in pdus) {
                val pduBytes = pdu as? ByteArray ?: continue
                val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(pduBytes, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pduBytes)
                }
                
                val body = message.messageBody ?: continue
                val sender = message.originatingAddress
                
                Log.d("SmsReceiver", "Received SMS from $sender: $body")
                
                // Parse transaction
                val parsedTx = SmsParser.parse(body, sender) ?: continue
                
                // Set the correct user ID if logged in
                val session = sessionManager.getSession()
                
                val isNotable = SmsParser.isNotable(body, sender)
                val finalTx = parsedTx.copy(
                    userId = session?.userId ?: "local_user",
                    isConfirmed = true,
                    note = if (isNotable) parsedTx.note else "SYSTEM_UNNOTED"
                )
                
                // Save to Room DB asynchronously
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.transactionDao()
                    
                    dao.insertTransaction(TransactionEntity.fromDomain(finalTx, isSynced = false))
                    Log.d("SmsReceiver", "Saved parsed auto-confirmed transaction: ${finalTx.merchant} - ₹${finalTx.amount}")
                    
                    // Trigger widget update
                    DebtTrackWidgetProvider.triggerUpdate(context)
                    
                    // Show notification for any parsed transaction
                    TransactionNotificationService.showAutoSaveNotification(
                        context = context,
                        transactionId = finalTx.id,
                        merchant = finalTx.merchant ?: "Merchant",
                        amount = finalTx.amount,
                        rawMessage = body,
                        time = finalTx.time
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error receiving SMS", e)
        }
    }
}
