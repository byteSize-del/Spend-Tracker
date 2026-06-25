package com.example.data.parser

import com.example.domain.model.MessageSource
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object SmsParser {
    private val DEBIT_KEYWORDS = listOf("debited", "spent", "paid", "charged", "deducted", "withdrawn", "sent")
    private val CREDIT_KEYWORDS = listOf("credited", "received", "added to", "deposited", "transferred from", "received from")
    private val OTP_KEYWORDS = listOf("otp", "one-time", "one time", "verification", "do not share", "secret")

    private val AMOUNT_REGEX = Regex("(?:Rs\\.?|INR|₹)\\s?(\\d+[,\\d]*\\.?\\d*)", RegexOption.IGNORE_CASE)
    private val DATE_REGEX = Regex("\\b(\\d{1,2}[-\\/]\\w{3,9}[-\\/]\\d{2,4})\\b|\\b(\\d{2}-\\w{3}-\\d{4})\\b|\\b(\\d{1,2}[-\\/]\\d{1,2}[-\\/]\\d{2,4})\\b")
    private val MERCHANT_REGEX = Regex("(?:at|to|towards|vpa)\\s+([A-Z][A-Za-z0-9\\s\\-&.]+)", RegexOption.IGNORE_CASE)
    private val REF_ID_REGEX = Regex("(?:Ref\\.?No\\.?|TXN|UPI Ref|Ref|ID)\\s*[:\\-]?\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE)
    private val BANK_REGEX = Regex("\\b(SBI|HDFC|ICICI|Axis|Kotak|IDBI|PNB|BOI|Union|Paytm|PhonePe|GPay|Bank|Bnk|Canara|Federal|Baroda|Yes Bank|Yesbank|IndusInd|HSBC|Citi)\\b", RegexOption.IGNORE_CASE)

    fun isCreditTransaction(text: String): Boolean {
        val lower = text.lowercase()
        return CREDIT_KEYWORDS.any { lower.contains(it) }
    }

    fun isNotable(body: String, sender: String?): Boolean {
        val fullText = "$body ${sender ?: ""}".lowercase()

        // 1. Account number check
        val hasAcct = fullText.contains("a/c") ||
                fullText.contains("acct") ||
                fullText.contains("account") ||
                fullText.contains("ending in") ||
                fullText.contains("ending with") ||
                fullText.contains("card") ||
                Regex("\\b[xX*]+\\d{2,}\\b").containsMatchIn(fullText) ||
                Regex("\\b\\d{4,}\\b").containsMatchIn(fullText)

        // 2. Bank or paying app check
        val hasBankOrApp = fullText.contains("sbi") ||
                fullText.contains("hdfc") ||
                fullText.contains("icici") ||
                fullText.contains("axis") ||
                fullText.contains("kotak") ||
                fullText.contains("idbi") ||
                fullText.contains("pnb") ||
                fullText.contains("boi") ||
                fullText.contains("union") ||
                fullText.contains("paytm") ||
                fullText.contains("phonepe") ||
                fullText.contains("phone pay") ||
                fullText.contains("gpay") ||
                fullText.contains("google pay") ||
                fullText.contains("bhim") ||
                fullText.contains("upi") ||
                fullText.contains("amazon pay") ||
                fullText.contains("paypal") ||
                fullText.contains("cred") ||
                fullText.contains("mpesa") ||
                fullText.contains("m-pesa") ||
                fullText.contains("mobikwik") ||
                fullText.contains("freecharge") ||
                fullText.contains("slice") ||
                fullText.contains("jupiter") ||
                fullText.contains("airtel money") ||
                fullText.contains("airtel payment") ||
                fullText.contains("jiomoney") ||
                fullText.contains("bank") ||
                fullText.contains("canara") ||
                fullText.contains("yes bank") ||
                fullText.contains("yesbank") ||
                fullText.contains("indusind") ||
                fullText.contains("hsbc") ||
                fullText.contains("citi") ||
                fullText.contains("federal bank") ||
                fullText.contains("baroda")

        // 3. Reference or Transaction ID check
        val hasRefId = fullText.contains("ref") ||
                fullText.contains("txn") ||
                fullText.contains("transaction") ||
                fullText.contains("txnid") ||
                fullText.contains("reference") ||
                fullText.contains("id:") ||
                Regex("(?:Ref\\.?No\\.?|TXN|UPI Ref|Ref|ID)\\s*[:\\-]?\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE).containsMatchIn(fullText)

        return hasAcct || hasBankOrApp || hasRefId
    }

    fun parse(messageBody: String, sender: String? = null): Transaction? {
        val lowerBody = messageBody.lowercase()

        // 1. Ignore if contains OTP keywords
        for (otp in OTP_KEYWORDS) {
            if (lowerBody.contains(otp)) return null
        }

        // 2. Ignore if does not contain debit/credit keywords
        var isDebit = false
        var isCredit = false
        for (debit in DEBIT_KEYWORDS) {
            if (lowerBody.contains(debit)) {
                isDebit = true
                break
            }
        }
        for (credit in CREDIT_KEYWORDS) {
            if (lowerBody.contains(credit)) {
                isCredit = true
                break
            }
        }
        if (!isDebit && !isCredit) return null

        // 3. Extract Amount
        var amount = 0.0
        val amountMatch = AMOUNT_REGEX.find(messageBody)
        if (amountMatch != null) {
            val amountStr = amountMatch.groupValues[1].replace(",", "")
            amount = amountStr.toDoubleOrNull() ?: 0.0
        }

        // 4. Extract Date
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var dateStr = today
        val dateMatch = DATE_REGEX.find(messageBody)
        if (dateMatch != null) {
            val rawDate = dateMatch.value
            dateStr = parseAndFormatDate(rawDate) ?: today
        }

        // 5. Extract Time
        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // 6. Extract Merchant
        var merchant: String? = null
        val merchantMatch = MERCHANT_REGEX.find(messageBody)
        if (merchantMatch != null) {
            merchant = merchantMatch.groupValues[1].trim()
            if (merchant.endsWith(".") || merchant.endsWith(",")) {
                merchant = merchant.substring(0, merchant.length - 1).trim()
            }
        } else {
            if (sender != null && sender.length > 3) {
                merchant = sender.replace("-", "").trim()
            }
        }

        // 7. Extract Reference ID
        var refId: String? = null
        val refMatch = REF_ID_REGEX.find(messageBody)
        if (refMatch != null) {
            refId = refMatch.groupValues[1].trim()
        }

        // 8. Extract Bank Name
        var bankName: String? = null
        val bankMatch = BANK_REGEX.find(messageBody)
        if (bankMatch != null) {
            bankName = bankMatch.groupValues[0].uppercase()
        }

        // 9. Auto Categorize
        val category = autoCategorize(merchant, messageBody)

        val transactionId = UUID.randomUUID().toString()

        return Transaction(
            id = transactionId,
            userId = "",
            amount = amount,
            currency = "INR",
            merchant = merchant ?: "Unknown Merchant",
            category = category,
            date = dateStr,
            time = nowTime,
            referenceId = refId,
            bankName = bankName,
            note = null,
            rawMessage = messageBody,
            source = MessageSource.SMS,
            isConfirmed = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun parseAndFormatDate(rawDate: String): String? {
        val formats = listOf(
            "dd-MMM-yyyy", "dd-MMM-yy", "dd/MMM/yyyy", "dd/MMM/yy",
            "dd-MM-yyyy", "dd/MM/yyyy", "dd-MM-yy", "dd/MM/yy",
            "yyyy-MM-dd", "dd MMM yyyy", "dd MMM yy"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(rawDate)
                if (date != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                // Ignore and try next
            }
        }
        return null
    }

    private fun autoCategorize(merchant: String?, body: String): TransactionCategory {
        val text = (merchant ?: "").lowercase() + " " + body.lowercase()
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
}
