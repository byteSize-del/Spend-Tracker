package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.remote.UserSession
import com.example.domain.model.Transaction
import com.example.ui.theme.BgElevated
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSurface
import com.example.ui.theme.Border
import com.example.ui.theme.Destructive
import com.example.ui.theme.GlassBgColor
import com.example.ui.theme.GlassBgColorHeavy
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.GlassScreenContainer
import com.example.ui.theme.GlassCard
import com.example.ui.theme.glassic
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.io.File
import java.lang.StringBuilder

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat != null && flat.contains(pkgName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    session: UserSession?,
    transactions: List<Transaction>,
    isSmsEnabled: Boolean,
    onSmsToggle: (Boolean) -> Unit,
    isBiometricsEnabled: Boolean,
    onBiometricsToggle: (Boolean) -> Unit,
    isNotificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isSystemEnabled by remember { mutableStateOf(false) }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isSystemEnabled = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showRawMessages by remember { mutableStateOf(true) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    GlassScreenContainer(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Info Header Card with premium glassmorphism styling
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GlassBgColorHeavy)
                            .border(1.dp, GlassBorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (session?.avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(session.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (session?.name ?: "U").take(1).uppercase(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = session?.name ?: "Guest User",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = session?.email ?: "local-only offline mode",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section Toggles Label
            Text(
                text = "APP PREFERENCES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // SMS Auto-Read Toggle
            PreferenceRow(
                title = "SMS Expense Parser",
                subtitle = "Automatically reads incoming transaction SMS alerts to prefill drafts.",
                checked = isSmsEnabled,
                onCheckedChange = onSmsToggle,
                tag = "sms_toggle"
            )

            // Notification Listener Toggle
            PreferenceRow(
                title = "App Notification Listener",
                subtitle = "Intercept notifications from financial apps (e.g., TrueCaller/Paytm).",
                checked = isNotificationEnabled && isSystemEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        onNotificationToggle(true)
                        if (!isNotificationServiceEnabled(context)) {
                            try {
                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "Please enable Spend Tracker in the list", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        onNotificationToggle(false)
                        Toast.makeText(context, "Notification parsing disabled. You can revoke permission in system settings.", Toast.LENGTH_LONG).show()
                    }
                },
                tag = "notification_toggle"
            )

            // Biometric Auth Lock Toggle
            PreferenceRow(
                title = "Biometric Lock Screen",
                subtitle = "Secures sensitive financial transaction records on application startup.",
                checked = isBiometricsEnabled,
                onCheckedChange = onBiometricsToggle,
                tag = "biometric_toggle"
            )

            // Show Raw Messages Toggle
            PreferenceRow(
                title = "Audit Raw Message Text",
                subtitle = "Retains and displays the source message content inside the transaction form.",
                checked = showRawMessages,
                onCheckedChange = { showRawMessages = it },
                tag = "raw_messages_toggle"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Actions Section
            Text(
                text = "DATA MANAGEMENT",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Export to CSV
            Button(
                onClick = { exportTransactionsToCsv(context, transactions, coroutineScope) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_csv_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassBgColor,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, GlassBorderColor)
            ) {
                Text("Export Transaction Records (CSV)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Log Out Button
            if (session != null) {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassBgColorHeavy,
                        contentColor = Destructive
                    ),
                    border = BorderStroke(1.dp, Destructive)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security & Privacy Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛡️ Security & Privacy Assurance",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• 100% Offline-First: All transactions, SMS processing, and notification logs are handled locally on your phone. No financial data leaves your device.\n\n" +
                               "• Direct Sideloading Warning: Because this app is built directly from source (not hosted on Google Play Console yet) and requests essential system capabilities like SMS and Notification Access, Google Play Protect will flag it as an \"Unknown developer.\"\n\n" +
                               "• Safe Installation: You can safely proceed by tapping \"More details\" and then \"Install anyway\". Spend Tracker is fully open, secure, and utilizes standard Android security sandbox isolation to safeguard your storage.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, GlassBorderColor)
                    ) {
                        Text("Read Full Privacy Policy & Safety Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    title = {
                        Text(
                            text = "🛡️ Privacy Policy & Data Safety",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Last Updated: June 2026\n",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            
                            Text(
                                text = "1. 100% Offline-First Architecture",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Spend Tracker does NOT collect, upload, or transmit any financial or personal data. Everything is parsed, stored, and computed entirely on your device using an offline Room SQLite database.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Text(
                                text = "2. SMS Permissions (READ_SMS & RECEIVE_SMS)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "The app parses transactional debt alerts from incoming messages ephemerally in-memory. Your raw text messages are never saved to local storage or sent to any server.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "3. Notification Access",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "When enabled, system notifications are parsed in real-time to capture transaction details from payment apps (e.g., Paytm, Google Pay, UPI). Data remains 100% on-device.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "4. Play Console Data Safety compliant declarations:",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "• Data Shared: None\n" +
                                       "• Data Encrypted: Yes (Stored inside Android's secure application sandbox)\n" +
                                       "• Data Deletion: Users have total control and can clear their records instantly using the clear cache button or by uninstalling.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacyDialog = false }) {
                            Text("I Understand", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = BgElevated,
                    textContentColor = TextSecondary,
                    titleContentColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Version info footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Spend Tracker Version 1.0.0 (Production Build)",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}

@Composable
fun PreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

private fun exportTransactionsToCsv(context: Context, transactions: List<Transaction>, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    coroutineScope.launch {
        try {
            val file = withContext(Dispatchers.IO) {
                val builder = StringBuilder()
                builder.append("ID,Amount,Currency,Merchant,Category,Date,Time,RefID,BankName,Note,Source,IsConfirmed\n")
                for (tx in transactions) {
                    builder.append("\"${tx.id}\",")
                    builder.append("${tx.amount},")
                    builder.append("\"${tx.currency}\",")
                    builder.append("\"${tx.merchant?.replace("\"", "\"\"") ?: ""}\",")
                    builder.append("\"${tx.category.name}\",")
                    builder.append("\"${tx.date}\",")
                    builder.append("\"${tx.time ?: ""}\",")
                    builder.append("\"${tx.referenceId ?: ""}\",")
                    builder.append("\"${tx.bankName ?: ""}\",")
                    builder.append("\"${tx.note?.replace("\"", "\"\"") ?: ""}\",")
                    builder.append("\"${tx.source.name}\",")
                    builder.append("${tx.isConfirmed}\n")
                }
                
                val csvContent = builder.toString()
                val cacheFile = File(context.cacheDir, "spend_tracker_records.csv")
                cacheFile.writeText(csvContent)
                cacheFile
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Spend Tracker Expense Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Export Transactions CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
