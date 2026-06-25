package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.remote.UserSession
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionCategory
import com.example.ui.theme.BgElevated
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSurface
import com.example.ui.theme.Border
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
import java.util.Locale

@Composable
fun DashboardScreen(
    session: UserSession?,
    transactions: List<Transaction>,
    unconfirmed: List<Transaction>,
    monthlyTotal: Double,
    sparkline: List<Double>,
    syncing: Boolean,
    onSync: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToEdit: (String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    GlassScreenContainer(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToEdit(null) },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spend Tracker",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = session?.email ?: "Offline mode",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSync,
                        enabled = !syncing,
                        modifier = Modifier.testTag("sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Data",
                            tint = if (syncing) TextMuted else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // User Avatar / Circle Placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassBgColorHeavy)
                            .border(2.dp, GlassBorderColor, CircleShape)
                            .clickable { onNavigateToProfile() }
                            .testTag("profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (session?.avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(session.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (session?.name ?: "U").take(1).uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Monthly Summary Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("summary_card"),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp)
                ) {
                    Text(
                        text = "THIS MONTH",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val wholeAmount = monthlyTotal.toLong()
                    val fractionAmount = ((monthlyTotal - wholeAmount) * 100).toInt()
                    val formattedWhole = String.format(Locale.US, "%,d", wholeAmount)
                    val formattedFraction = String.format(Locale.US, "%02d", fractionAmount)

                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "₹$formattedWhole",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = ".$formattedFraction",
                            color = TextSecondary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // 7-day sparkline or trend bars
                    val trendPct = if (sparkline.size >= 2) {
                        val first = sparkline.first()
                        val last = sparkline.last()
                        if (first > 0) {
                            ((last - first) / first) * 100
                        } else {
                            0.0
                        }
                    } else {
                        0.0
                    }
                    val trendText = if (trendPct >= 0) {
                        String.format(Locale.US, "+%.0f%% vs last week", trendPct)
                    } else {
                        String.format(Locale.US, "%.0f%% vs last week", trendPct)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Sparkline(
                            data = sparkline,
                            modifier = Modifier
                                .width(90.dp)
                                .height(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = trendText,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pending review banner
            AnimatedVisibility(visible = unconfirmed.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .glassic(
                            shape = RoundedCornerShape(16.dp),
                            borderBrush = SolidColor(PendingColor.copy(alpha = 0.5f))
                        )
                        .clickable { onNavigateToEdit(unconfirmed.first().id) }
                        .testTag("pending_review_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left vertical indicator
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(PendingColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Yellow/Amber Alert circular icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PendingColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Pending Review",
                                tint = PendingColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${unconfirmed.size} New Transaction Detected",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val firstUnconfirmed = unconfirmed.firstOrNull()
                            val subtitle = if (firstUnconfirmed != null) {
                                "₹${firstUnconfirmed.amount} at ${firstUnconfirmed.merchant ?: "Unknown"} · Review now"
                            } else {
                                "Tap to review auto-detected alerts."
                            }
                            Text(
                                text = subtitle,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Header for Transaction List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECORDS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "See All",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToHistory() }
                        .testTag("see_all_button")
                )
            }

            // Transaction List
            val visibleTransactions = remember(transactions) {
                transactions.filter { it.note != "SYSTEM_UNNOTED" }
            }

            if (visibleTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No expenses recorded yet",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Your SMS alerts will automatically appear here.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                // Group by date
                val grouped = visibleTransactions.groupBy { it.date }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    grouped.forEach { (date, txList) ->
                        item {
                            Text(
                                text = formatHeaderDate(date),
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(txList, key = { it.id }) { item ->
                            TransactionRow(
                                transaction = item,
                                onClick = { onNavigateToEdit(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun Sparkline(data: List<Double>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val maxVal = (data.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val points = if (data.isEmpty()) List(7) { 0.1 } else data
        points.forEach { value ->
            val heightFraction = (value / maxVal).coerceIn(0.1, 1.0).toFloat()
            val isMax = (value == points.maxOrNull() && value > 0)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction)
                    .background(
                        color = if (isMax) Color.White.copy(alpha = 0.9f) else GlassBorderColor,
                        shape = RoundedCornerShape(100)
                    )
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderBrush = if (!transaction.isConfirmed) {
        SolidColor(PendingColor.copy(alpha = 0.8f))
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .glassic(
                shape = RoundedCornerShape(16.dp),
                borderBrush = borderBrush
            )
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Emoji Icon Sphere with fine border
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GlassBgColorHeavy)
                    .border(1.dp, GlassBorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(transaction.category),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: "Unknown Payee",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val catText = transaction.category.name.lowercase().replaceFirstChar { it.uppercase() }
                val timeStr = transaction.time ?: "Manual"
                Text(
                    text = "$catText • $timeStr",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!transaction.isConfirmed) {
                    Text(
                        text = "Review",
                        color = PendingColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getCategoryEmoji(category: TransactionCategory): String = when (category) {
    TransactionCategory.FOOD -> "🍔"
    TransactionCategory.TRANSPORT -> "🚗"
    TransactionCategory.SHOPPING -> "🛍️"
    TransactionCategory.UTILITIES -> "⚡"
    TransactionCategory.ENTERTAINMENT -> "🎬"
    TransactionCategory.HEALTH -> "🏥"
    TransactionCategory.EDUCATION -> "🎓"
    TransactionCategory.RENT -> "🏠"
    TransactionCategory.TRANSFER -> "💸"
    TransactionCategory.OTHER -> "📝"
}

private fun formatHeaderDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr) ?: return dateStr
        val formatter = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        dateStr
    }
}
