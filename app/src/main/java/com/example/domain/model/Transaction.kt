package com.example.domain.model

enum class TransactionCategory {
    FOOD, TRANSPORT, SHOPPING, UTILITIES, ENTERTAINMENT,
    HEALTH, EDUCATION, RENT, TRANSFER, OTHER
}

enum class MessageSource {
    SMS, NOTIFICATION, MANUAL
}

data class Transaction(
    val id: String,                   // UUID
    val userId: String,               // Supabase user ID
    val amount: Double,               // Parsed or manually entered
    val currency: String = "INR",
    val merchant: String?,            // Where money was spent
    val category: TransactionCategory,
    val date: String,                 // YYYY-MM-DD
    val time: String?,                // HH:mm:ss or HH:mm
    val referenceId: String?,         // Bank/UPI reference number
    val bankName: String?,
    val note: String?,                // User-added notes
    val rawMessage: String?,          // Original SMS body
    val source: MessageSource,        // SMS / NOTIFICATION / MANUAL
    val isConfirmed: Boolean,         // User has reviewed and saved
    val createdAt: Long,              // Epoch milliseconds
    val updatedAt: Long               // Epoch milliseconds
)
