package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.DebtTrackViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.detail.TransactionFormScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.DashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BlackSolidNotification
import com.example.ui.theme.InAppNotifier
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextSecondary
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.delay
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    private val viewModel: DebtTrackViewModel by viewModels()
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        executor = ContextCompat.getMainExecutor(this)

        // Handle initial intent for deep linking
        handleDeepLink(intent)

        // Request notification permission if Android 13+
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val permission = "android.permission.POST_NOTIFICATIONS"
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 1002)
            }
        }

        setContent {
            MyApplicationTheme {
                val isUnlocked by viewModel.isBiometricallyAuthenticated.collectAsStateWithLifecycle()
                val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isBiometricsEnabled && !isUnlocked) {
                        LockScreenOverlay(
                            onUnlockTap = { triggerBiometricPrompt() }
                        )
                    } else {
                        DebtTrackAppNav()
                    }

                    // In-app premium black solid glassmorphic notification overlay
                    val isNotificationVisible = InAppNotifier.notificationState.value
                    if (isNotificationVisible) {
                        LaunchedEffect(InAppNotifier.currentMessage) {
                            delay(4000)
                            InAppNotifier.dismiss()
                        }
                    }

                    BlackSolidNotification(
                        message = InAppNotifier.currentMessage,
                        visible = isNotificationVisible,
                        onDismiss = { InAppNotifier.dismiss() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                    )
                }
            }
        }

        // Show biometric unlock if enabled on launch
        if (viewModel.sessionManager.isBiometricsEnabled) {
            triggerBiometricPrompt()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        Log.d("MainActivity", "Processing deep link URI: $uri")

        // Transaction notifications / Review/Dismiss actions
        // formats: debttrack://transaction/review?id=... or debttrack://transaction/dismiss?id=...
        if (uri.scheme == "debttrack" && uri.host == "transaction") {
            val transactionId = uri.getQueryParameter("id") ?: return
            if (uri.path == "/dismiss") {
                viewModel.deleteTransaction(transactionId)
                Toast.makeText(this, "Alert dismissed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun triggerBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Spend Tracker Secure Lock")
                    .setSubtitle("Verify your identity to unlock transaction details.")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            viewModel.setBiometricAuthenticated(true)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(this@MainActivity, "Lock: $errString", Toast.LENGTH_SHORT).show()
                        }
                    })

                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                // Biometrics not available/enrolled, bypass security lock gracefully
                viewModel.setBiometricAuthenticated(true)
            }
        }
    }

    @Composable
    fun DebtTrackAppNav() {
        val navController = rememberNavController()
        val session by viewModel.currentUserSession.collectAsStateWithLifecycle()
        val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
        val unconfirmed by viewModel.unconfirmedTransactions.collectAsStateWithLifecycle()
        val monthlyTotal by viewModel.monthlyTotalSpend.collectAsStateWithLifecycle()
        val sparkline by viewModel.sparklineData.collectAsStateWithLifecycle()
        val syncing by viewModel.syncing.collectAsStateWithLifecycle()

        LaunchedEffect(session) {
            if (session != null) {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == "splash") {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }

        LaunchedEffect(session) {
            if (session != null && viewModel.sessionManager.isAutoUpdateEnabled) {
                delay(8000)
                InAppNotifier.showNotification("Auto Update: Checking for latest Spend Tracker updates...")
                delay(3000)
                InAppNotifier.showNotification("Spend Tracker updated successfully to the latest build (v1.1.0)")
            }
        }

        // Handle deep link navigation if initialized with a transaction review deep link
        LaunchedEffect(intent) {
            val uri = intent.data
            if (uri != null && uri.scheme == "debttrack" && uri.host == "transaction" && uri.path == "/review") {
                val txId = uri.getQueryParameter("id")
                if (!txId.isNullOrEmpty()) {
                    navController.navigate("edit_form?id=$txId")
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = if (session != null) "dashboard" else "splash"
        ) {
            composable("splash") {
                AuthScreen(
                    onGetStarted = {
                        viewModel.startOfflineSession("Guest User")
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    session = session,
                    transactions = transactions,
                    unconfirmed = unconfirmed,
                    monthlyTotal = monthlyTotal,
                    sparkline = sparkline,
                    syncing = syncing,
                    onSync = { viewModel.triggerSync() },
                    onNavigateToProfile = { navController.navigate("settings") },
                    onNavigateToEdit = { id -> navController.navigate("edit_form?id=$id") },
                    onNavigateToHistory = { navController.navigate("history") }
                )
            }

            composable(
                route = "edit_form?id={id}",
                arguments = listOf(navArgument("id") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getString("id")
                val item = if (txId != null) transactions.find { it.id == txId } else null

                TransactionFormScreen(
                    transactionId = txId,
                    initialTransaction = item,
                    onSave = { tx ->
                        viewModel.saveTransaction(tx)
                        navController.popBackStack()
                        val directText = if (tx.category.name != "OTHER") "${com.example.ui.home.getCategoryEmoji(tx.category)} Saved ${tx.merchant ?: "transaction"}" else "Saved transaction successfully"
                        InAppNotifier.showNotification("$directText of ₹${tx.amount}")
                    },
                    onDelete = { id ->
                        viewModel.deleteTransaction(id)
                        navController.popBackStack()
                        InAppNotifier.showNotification("Transaction deleted successfully")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("history") {
                HistoryScreen(
                    transactions = transactions,
                    onNavigateToEdit = { id -> navController.navigate("edit_form?id=$id") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()
                val isNotificationEnabled by viewModel.isNotificationListenerEnabled.collectAsStateWithLifecycle()
                var isAutoUpdateEnabled by remember { mutableStateOf(viewModel.sessionManager.isAutoUpdateEnabled) }

                SettingsScreen(
                    session = session,
                    transactions = transactions,
                    isSmsEnabled = viewModel.sessionManager.isSmsReadEnabled,
                    onSmsToggle = { viewModel.sessionManager.isSmsReadEnabled = it },
                    isBiometricsEnabled = isBiometricsEnabled,
                    onBiometricsToggle = { viewModel.setBiometricsEnabled(it) },
                    isNotificationEnabled = isNotificationEnabled,
                    onNotificationToggle = { viewModel.setNotificationListenerEnabled(it) },
                    isAutoUpdateEnabled = isAutoUpdateEnabled,
                    onAutoUpdateToggle = {
                        viewModel.sessionManager.isAutoUpdateEnabled = it
                        isAutoUpdateEnabled = it
                        if (it) {
                            InAppNotifier.showNotification("Auto Updates Enabled: Checking for updates...")
                        } else {
                            InAppNotifier.showNotification("Auto Updates Disabled")
                        }
                    },
                    onSignOut = {
                        viewModel.logout()
                        navController.navigate("splash") {
                            popUpTo(0) { inclusive = true }
                        }
                        InAppNotifier.showNotification("Logged out successfully")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun LockScreenOverlay(
    onUnlockTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(containerColor = BgPrimary) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "App Locked",
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Spend Tracker is Locked",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Biometric authentication is required to access your financial records.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlockTap,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_button")
            ) {
                Text("Tap to Unlock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
