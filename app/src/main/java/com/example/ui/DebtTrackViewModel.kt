package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.remote.SessionManager
import com.example.data.remote.UserSession
import com.example.data.repository.AppRepository
import com.example.domain.model.Transaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.service.DebtTrackWidgetProvider

class DebtTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.transactionDao()
    val sessionManager = SessionManager(application)
    val appRepository = AppRepository(dao, sessionManager)

    // UI States
    val allTransactions: StateFlow<List<Transaction>> = appRepository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unconfirmedTransactions: StateFlow<List<Transaction>> = appRepository.unconfirmedTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isBiometricallyAuthenticated = MutableStateFlow(false)
    val isBiometricallyAuthenticated = _isBiometricallyAuthenticated.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()

    private val _isBiometricsEnabled = MutableStateFlow(sessionManager.isBiometricsEnabled)
    val isBiometricsEnabled = _isBiometricsEnabled.asStateFlow()

    private val _isNotificationListenerEnabled = MutableStateFlow(sessionManager.isNotificationListenerEnabled)
    val isNotificationListenerEnabled = _isNotificationListenerEnabled.asStateFlow()

    private val _currentUserSession = MutableStateFlow<UserSession?>(sessionManager.getSession())
    val currentUserSession = _currentUserSession.asStateFlow()

    init {
        // If biometrics are NOT enabled, authenticate by default
        if (!sessionManager.isBiometricsEnabled) {
            _isBiometricallyAuthenticated.value = true
        }
        triggerSync()
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        sessionManager.isBiometricsEnabled = enabled
        _isBiometricsEnabled.value = enabled
        if (!enabled) {
            _isBiometricallyAuthenticated.value = true
        }
    }

    fun setNotificationListenerEnabled(enabled: Boolean) {
        sessionManager.isNotificationListenerEnabled = enabled
        _isNotificationListenerEnabled.value = enabled
    }

    fun setBiometricAuthenticated(auth: Boolean) {
        _isBiometricallyAuthenticated.value = auth
    }

    fun startOfflineSession(username: String) {
        viewModelScope.launch {
            sessionManager.saveOfflineSession(username)
            _currentUserSession.value = sessionManager.getSession()
            triggerSync()
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _currentUserSession.value = null
            appRepository.clearLocalData()
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncing.value = true
            delay(300) // Simulated fast local-database sync/refresh
            _syncing.value = false
        }
    }

    fun confirmTransaction(transaction: Transaction) {
        viewModelScope.launch {
            appRepository.updateTransaction(transaction.copy(isConfirmed = true))
            DebtTrackWidgetProvider.triggerUpdate(getApplication())
            com.example.service.TransactionNotificationService.showNotification(
                context = getApplication(),
                title = "Transaction Confirmed",
                bodyText = "Confirmed transaction of ₹${transaction.amount} at ${transaction.merchant ?: "Unknown Payee"}",
                transactionId = transaction.id
            )
        }
    }

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val isNew = transaction.id.isEmpty() || transaction.id == "null"
            val finalTx = if (isNew) {
                transaction.copy(
                    id = UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                transaction
            }
            if (isNew) {
                appRepository.insertTransaction(finalTx)
            } else {
                appRepository.updateTransaction(finalTx)
            }
            DebtTrackWidgetProvider.triggerUpdate(getApplication())
            val actionWord = if (isNew) "Recorded" else "Updated"
            com.example.service.TransactionNotificationService.showNotification(
                context = getApplication(),
                title = "Transaction $actionWord",
                bodyText = "$actionWord transaction of ₹${finalTx.amount} for ${finalTx.merchant ?: "Unknown Payee"}",
                transactionId = finalTx.id
            )
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            appRepository.deleteTransaction(id)
            DebtTrackWidgetProvider.triggerUpdate(getApplication())
        }
    }

    // Calculations for monthly summary
    val monthlyTotalSpend: StateFlow<Double> = allTransactions.map { list ->
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        list.filter { it.isConfirmed && it.date.startsWith(currentMonthStr) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val sparklineData: StateFlow<List<Double>> = allTransactions.map { list ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val dates = mutableListOf<String>()
        for (i in 0..6) {
            dates.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        dates.reverse()

        dates.map { dateStr ->
            list.filter { it.isConfirmed && it.date == dateStr }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0.0 })
}
