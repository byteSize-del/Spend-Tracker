package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.MessageSource
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionCategory

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date", "time"]),
        Index(value = ["isConfirmed"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val currency: String,
    val merchant: String?,
    val category: String,
    val date: String,
    val time: String?,
    val referenceId: String?,
    val bankName: String?,
    val note: String?,
    val rawMessage: String?,
    val source: String,
    val isConfirmed: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = false
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        userId = userId,
        amount = amount,
        currency = currency,
        merchant = merchant,
        category = try { TransactionCategory.valueOf(category) } catch (e: Exception) { TransactionCategory.OTHER },
        date = date,
        time = time,
        referenceId = referenceId,
        bankName = bankName,
        note = note,
        rawMessage = rawMessage,
        source = try { MessageSource.valueOf(source) } catch (e: Exception) { MessageSource.MANUAL },
        isConfirmed = isConfirmed,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(transaction: Transaction, isSynced: Boolean = false): TransactionEntity = TransactionEntity(
            id = transaction.id,
            userId = transaction.userId,
            amount = transaction.amount,
            currency = transaction.currency,
            merchant = transaction.merchant,
            category = transaction.category.name,
            date = transaction.date,
            time = transaction.time,
            referenceId = transaction.referenceId,
            bankName = transaction.bankName,
            note = transaction.note,
            rawMessage = transaction.rawMessage,
            source = transaction.source.name,
            isConfirmed = transaction.isConfirmed,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt,
            isSynced = isSynced
        )
    }
}
