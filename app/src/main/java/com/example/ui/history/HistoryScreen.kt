package com.example.ui.history

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MessageSource
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionCategory
import com.example.ui.detail.OutlinedTextFieldDefaults
import com.example.ui.home.TransactionRow
import com.example.ui.home.getCategoryEmoji
import com.example.ui.theme.BgPrimary
import com.example.ui.theme.BgSurface
import com.example.ui.theme.Border
import com.example.ui.theme.GlassBgColor
import com.example.ui.theme.GlassBgColorHeavy
import com.example.ui.theme.GlassBorderColor
import com.example.ui.theme.GlassScreenContainer
import com.example.ui.theme.GlassCard
import com.example.ui.theme.glassic
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<Transaction>,
    onNavigateToEdit: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var selectedSource by remember { mutableStateOf<MessageSource?>(null) }
    var selectedDateRange by remember { mutableStateOf("ALL") } // ALL, TODAY, THIS_WEEK, THIS_MONTH
    var sortBy by remember { mutableStateOf("NEWEST") } // NEWEST, OLDEST, HIGHEST_AMOUNT
    var showFilters by remember { mutableStateOf(false) }

    // Filtering logic
    val filteredTransactions = remember(transactions, searchQuery, selectedCategory, selectedSource, selectedDateRange, sortBy) {
        var list = transactions.filter { it.note != "SYSTEM_UNNOTED" }

        // Search Filter
        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            list = list.filter {
                (it.merchant ?: "").lowercase().contains(q) ||
                (it.note ?: "").lowercase().contains(q) ||
                (it.bankName ?: "").lowercase().contains(q) ||
                (it.referenceId ?: "").lowercase().contains(q)
            }
        }

        // Category Filter
        if (selectedCategory != null) {
            list = list.filter { it.category == selectedCategory }
        }

        // Source Filter
        if (selectedSource != null) {
            list = list.filter { it.source == selectedSource }
        }

        // Date Range Filter
        if (selectedDateRange != "ALL") {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val cal = Calendar.getInstance()

            when (selectedDateRange) {
                "TODAY" -> {
                    list = list.filter { it.date == todayStr }
                }
                "THIS_WEEK" -> {
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    val limitDate = sdf.format(cal.time)
                    list = list.filter { it.date >= limitDate }
                }
                "THIS_MONTH" -> {
                    val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                    list = list.filter { it.date.startsWith(currentMonthPrefix) }
                }
            }
        }

        // Sorting
        list = when (sortBy) {
            "OLDEST" -> list.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.time })
            "HIGHEST_AMOUNT" -> list.sortedByDescending { it.amount }
            else -> list.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.time }) // NEWEST
        }

        list
    }

    GlassScreenContainer(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Transaction History",
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
        val activeFiltersCount = (if (selectedDateRange != "ALL") 1 else 0) +
                (if (selectedSource != null) 1 else 0) +
                (if (sortBy != "NEWEST") 1 else 0)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search row with a clean "Filters" toggle button next to it
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search merchant, note, ref...", color = TextMuted, fontSize = 14.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .height(56.dp) // Perfect match with OutlinedTextField height
                        .then(
                            if (showFilters) {
                                Modifier
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                            } else {
                                Modifier.glassic(shape = RoundedCornerShape(16.dp))
                            }
                        )
                        .clickable { showFilters = !showFilters }
                        .padding(horizontal = 14.dp)
                        .testTag("filters_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Filters",
                            color = if (showFilters) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeFiltersCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        color = if (showFilters) Color.Black else Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeFiltersCount.toString(),
                                    color = if (showFilters) Color.White else Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick Category Select Row (always accessible and beautiful)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val selected = selectedCategory == null
                    Box(
                        modifier = Modifier
                            .then(
                                if (selected) {
                                    Modifier
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                                } else {
                                    Modifier.glassic(shape = RoundedCornerShape(16.dp))
                                }
                            )
                            .clickable { selectedCategory = null }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("category_filter_all")
                    ) {
                        Text(
                            text = "All Categories",
                            color = if (selected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(TransactionCategory.values(), key = { it.name }) { cat ->
                    val selected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .then(
                                if (selected) {
                                    Modifier
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                                } else {
                                    Modifier.glassic(shape = RoundedCornerShape(16.dp))
                                }
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("category_filter_${cat.name}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = getCategoryEmoji(cat) + " ", fontSize = 12.sp)
                            Text(
                                text = cat.name,
                                color = if (selected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Animated Collapsible Refined Filters Panel
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title row inside collapsible panel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REFINE TRANSACTIONS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            if (activeFiltersCount > 0) {
                                Text(
                                    text = "Clear Filters",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            selectedDateRange = "ALL"
                                            selectedSource = null
                                            sortBy = "NEWEST"
                                        }
                                        .testTag("clear_filters_button")
                                )
                            }
                        }

                        // 1. Date Range Section
                        Column {
                            Text(
                                text = "TIME PERIOD",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ALL", "TODAY", "THIS_WEEK", "THIS_MONTH").forEach { range ->
                                    val selected = selectedDateRange == range
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (selected) {
                                                    Modifier
                                                        .background(Color.White, RoundedCornerShape(12.dp))
                                                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                                                } else {
                                                    Modifier.glassic(shape = RoundedCornerShape(12.dp))
                                                }
                                            )
                                            .clickable { selectedDateRange = range }
                                            .padding(vertical = 8.dp)
                                            .testTag("date_filter_$range"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = range.replace("_", " "),
                                            color = if (selected) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Source Section
                        Column {
                            Text(
                                text = "TRANSACTION SOURCE",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ALL", "SMS", "MANUAL").forEach { src ->
                                    val enumSrc = if (src == "ALL") null else MessageSource.valueOf(src)
                                    val selected = selectedSource == enumSrc
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (selected) {
                                                    Modifier
                                                        .background(Color.White, RoundedCornerShape(12.dp))
                                                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                                                } else {
                                                    Modifier.glassic(shape = RoundedCornerShape(12.dp))
                                                }
                                            )
                                            .clickable { selectedSource = enumSrc }
                                            .padding(vertical = 8.dp)
                                            .testTag("source_filter_$src"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (src == "ALL") "ALL" else src,
                                            color = if (selected) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Sort By Section
                        Column {
                            Text(
                                text = "SORT BY",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("NEWEST", "OLDEST", "HIGHEST").forEach { sort ->
                                    val currentSort = if (sort == "HIGHEST") "HIGHEST_AMOUNT" else sort
                                    val selected = sortBy == currentSort
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (selected) {
                                                    Modifier
                                                        .background(Color.White, RoundedCornerShape(12.dp))
                                                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                                                } else {
                                                    Modifier.glassic(shape = RoundedCornerShape(12.dp))
                                                }
                                            )
                                            .clickable { sortBy = currentSort }
                                            .padding(vertical = 8.dp)
                                            .testTag("sort_filter_$sort"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (sort == "HIGHEST") "HIGHEST" else sort,
                                            color = if (selected) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results Label
            Text(
                text = "FOUND ${filteredTransactions.size} RECORDS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // History Transactions List
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔍", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No records match search criteria",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { item ->
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
