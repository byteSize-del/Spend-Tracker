package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.data.remote.SessionManager
import com.example.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AppRepository(
    private val transactionDao: TransactionDao,
    private val sessionManager: SessionManager
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)

    val unconfirmedTransactions: Flow<List<Transaction>> = transactionDao.getUnconfirmedTransactionsFlow()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)

    suspend fun insertTransaction(transaction: Transaction) {
        val session = sessionManager.getSession()
        val userId = session?.userId ?: "local_user"
        val txWithUser = transaction.copy(userId = userId)

        // Save locally
        val entity = TransactionEntity.fromDomain(txWithUser, isSynced = true)
        transactionDao.insertTransaction(entity)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        val session = sessionManager.getSession()
        val userId = session?.userId ?: transaction.userId.ifEmpty { "local_user" }
        val txWithUser = transaction.copy(userId = userId, updatedAt = System.currentTimeMillis())

        // Update locally
        val entity = TransactionEntity.fromDomain(txWithUser, isSynced = true)
        transactionDao.updateTransaction(entity)
    }

    suspend fun deleteTransaction(transactionId: String) {
        // Delete locally
        transactionDao.deleteTransactionById(transactionId)
    }

    suspend fun syncWithCloud(): Boolean {
        // No cloud sync needed, operating in high-performance local-first mode
        return true
    }

    suspend fun clearLocalData() {
        transactionDao.clearAllTransactions()
    }
}
