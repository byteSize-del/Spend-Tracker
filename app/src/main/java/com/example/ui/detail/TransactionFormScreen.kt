package com.example.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MessageSource
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionCategory
import com.example.ui.home.getCategoryEmoji
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
import androidx.compose.ui.graphics.SolidColor
import com.example.ui.theme.PendingColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    transactionId: String?,
    initialTransaction: Transaction?,
    onSave: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TransactionCategory.OTHER) }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var referenceId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(MessageSource.MANUAL) }
    var rawMessage by remember { mutableStateOf<String?>(null) }
    var isConfirmed by remember { mutableStateOf(false) }

    var rawMessageExpanded by remember { mutableStateOf(false) }

    // Prefill fields
    LaunchedEffect(initialTransaction, transactionId) {
        if (transactionId != null && initialTransaction != null) {
            amount = initialTransaction.amount.toString()
            merchant = initialTransaction.merchant ?: ""
            category = initialTransaction.category
            date = initialTransaction.date
            time = initialTransaction.time ?: ""
            bankName = initialTransaction.bankName ?: ""
            referenceId = initialTransaction.referenceId ?: ""
            note = initialTransaction.note ?: ""
            source = initialTransaction.source
            rawMessage = initialTransaction.rawMessage
            isConfirmed = initialTransaction.isConfirmed
        } else {
            // New transaction defaults
            amount = ""
            merchant = ""
            category = TransactionCategory.OTHER
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            bankName = ""
            referenceId = ""
            note = ""
            source = MessageSource.MANUAL
            rawMessage = null
            isConfirmed = true
        }
    }

    GlassScreenContainer(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (transactionId == null) "Add Record" else if (!isConfirmed) "Review Transaction" else "Edit Record",
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
            // Unconfirmed Banner Indicator
            if (transactionId != null && !isConfirmed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .glassic(
                            shape = RoundedCornerShape(8.dp),
                            borderBrush = SolidColor(PendingColor.copy(alpha = 0.5f))
                        )
                ) {
                    Text(
                        text = "⚡ Auto-detected from SMS alert. Please verify and save.",
                        color = PendingColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Amount Field (Large numeral)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AMOUNT (INR)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { 
                        Text(
                            "0.00", 
                            color = TextMuted, 
                            fontSize = 36.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        ) 
                    },
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        color = Color.White,
                        fontSize = 40.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merchant / Paid To
            Text(
                text = "MERCHANT / PAID TO",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                placeholder = { Text("e.g. Swiggy, Uber, Rent", color = TextMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merchant_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Horizontal Selector
            Text(
                text = "CATEGORY",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TransactionCategory.values()) { cat ->
                    val selected = cat == category
                    Box(
                        modifier = Modifier
                            .then(
                                if (selected) {
                                    Modifier
                                        .background(Color.White, RoundedCornerShape(20.dp))
                                        .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                                } else {
                                    Modifier.glassic(shape = RoundedCornerShape(20.dp))
                                }
                            )
                            .clickable { category = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("category_chip_${cat.name}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = getCategoryEmoji(cat) + " ", fontSize = 13.sp)
                            Text(
                                text = cat.name,
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DATE",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        placeholder = { Text("YYYY-MM-DD", color = TextMuted) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("date_input")
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TIME (OPTIONAL)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        placeholder = { Text("HH:MM", color = TextMuted) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("time_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bank Name & Reference ID
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BANK / SENDER",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        placeholder = { Text("e.g. HDFC, SBI", color = TextMuted) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bank_input")
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "REFERENCE ID",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = referenceId,
                        onValueChange = { referenceId = it },
                        placeholder = { Text("TXN / Ref No", color = TextMuted) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ref_id_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            Text(
                text = "NOTES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Add transaction details, tags, split billing...", color = TextMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults(),
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Read only metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOURCE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(BgSurface, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = source.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Raw SMS expander with custom Glassmorphism styling and code formatting
            if (!rawMessage.isNullOrEmpty()) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rawMessageExpanded = !rawMessageExpanded }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View original message",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (rawMessageExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        AnimatedVisibility(visible = rawMessageExpanded) {
                            Text(
                                text = rawMessage ?: "",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GlassBgColorHeavy)
                                    .padding(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ACTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (transactionId != null) {
                    // Delete Button
                    OutlinedButton(
                        onClick = { onDelete(transactionId) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("delete_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Destructive),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Destructive)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }

                // Save / Confirm Button
                Button(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                        val finalTransaction = Transaction(
                            id = transactionId ?: "",
                            userId = initialTransaction?.userId ?: "",
                            amount = parsedAmount,
                            currency = "INR",
                            merchant = merchant.ifEmpty { "Manual Entry" },
                            category = category,
                            date = date,
                            time = time.ifEmpty { null },
                            referenceId = referenceId.ifEmpty { null },
                            bankName = bankName.ifEmpty { null },
                            note = note.ifEmpty { null },
                            rawMessage = rawMessage,
                            source = source,
                            isConfirmed = true, // Force confirmed when saved
                            createdAt = initialTransaction?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(finalTransaction)
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .testTag("save_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (transactionId == null) "Save" else if (!isConfirmed) "Confirm & Save" else "Save Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun OutlinedTextFieldDefaults() = TextFieldDefaults.colors(
    focusedContainerColor = BgElevated,
    unfocusedContainerColor = BgSurface,
    disabledContainerColor = BgSurface,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedIndicatorColor = Color.White,
    unfocusedIndicatorColor = Border
)
