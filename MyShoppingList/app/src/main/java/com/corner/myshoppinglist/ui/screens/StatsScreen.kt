package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.dao.CategoryStat
import com.corner.myshoppinglist.data.local.dao.PeriodStat
import com.corner.myshoppinglist.viewmodel.ShoppingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: ShoppingViewModel,
    onNavigateBack: () -> Unit
) {
    val itemStats by viewModel.itemStats.collectAsStateWithLifecycle()
    val expensiveItems by viewModel.expensiveItems.collectAsStateWithLifecycle()
    val spentPerMonth by viewModel.spentPerMonth.collectAsStateWithLifecycle()
    val spentByCategory by viewModel.spentByCategory.collectAsStateWithLifecycle()
    val spentByStore by viewModel.spentByStore.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    val totalSpentInPeriod = spentPerMonth.sumOf { it.totalSpent }
    val avgSpentPerMonth = if (spentPerMonth.isNotEmpty()) totalSpentInPeriod / spentPerMonth.size else 0.0

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDateRange(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                    showDatePicker = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setDateRange(null, null)
                    showDatePicker = false
                }) {
                    Text("Clear")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights & Stats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = if (dateRange == null) Icons.Default.DateRange else Icons.Default.FilterList,
                            contentDescription = "Filter by date"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date Range Display
            if (dateRange != null) {
                item {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val start = sdf.format(Date(dateRange!!.first))
                    val end = sdf.format(Date(dateRange!!.second))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Period: $start - $end",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            TextButton(onClick = { viewModel.setDateRange(null, null) }) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            // KPI Summary Cards
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        KPICard(
                            label = if (dateRange == null) "Total Spent" else "Period Total",
                            value = "$currencySymbol${"%.0f".format(Locale.US, totalSpentInPeriod)}",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        KPICard(
                            label = "Monthly Avg",
                            value = "$currencySymbol${"%.0f".format(Locale.US, avgSpentPerMonth)}",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Spending Trend Chart
            if (spentPerMonth.isNotEmpty()) {
                item {
                    SectionHeader("Spending Trend")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SpendingBarChart(
                                data = spentPerMonth.take(6).reversed(),
                                primaryColor = MaterialTheme.colorScheme.primary,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }
            }

            // Category Breakdown Donut
            if (spentByCategory.isNotEmpty()) {
                item {
                    SectionHeader("Category Breakdown")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryDonutChart(
                                data = spentByCategory,
                                modifier = Modifier.size(150.dp)
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                spentByCategory.take(5).forEach { stat ->
                                    CategoryLegendItem(stat, currencySymbol)
                                }
                            }
                        }
                    }
                }
            }

            // Shop Spending
            if (spentByStore.isNotEmpty()) {
                item {
                    SectionHeader("Shop Spending")
                }
                items(spentByStore.take(5)) { stat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(stat.storeName) },
                            trailingContent = {
                                Text(
                                    "$currencySymbol${"%.2f".format(Locale.US, stat.totalSpent)}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                }
            }

            // Most Expensive Purchases
            if (expensiveItems.isNotEmpty()) {
                item {
                    SectionHeader("Most Expensive Purchases")
                }
                items(expensiveItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(item.itemName, fontWeight = FontWeight.Medium) },
                            trailingContent = {
                                Text(
                                    "$currencySymbol${"%.2f".format(Locale.US, item.maxPrice)}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
            }

            // Item Stats (Simplified)
            item {
                SectionHeader("Top Items by Spend")
            }
            items(itemStats.take(10)) { stat ->
                ListItem(
                    headlineContent = { Text(stat.itemName) },
                    supportingContent = { Text("Qty: ${"%.1f".format(Locale.US, stat.totalQuantity)}") },
                    trailingContent = {
                        Text(
                            "$currencySymbol${"%.2f".format(Locale.US, stat.totalSpent)}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun KPICard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SpendingBarChart(data: List<PeriodStat>, primaryColor: Color, currencySymbol: String) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val maxVal = data.maxOfOrNull { it.totalSpent } ?: 1.0
    
    Column {
        // Selection Detail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedIndex != -1 && selectedIndex < data.size) {
                val stat = data[selectedIndex]
                Text(
                    text = "${stat.period}: $currencySymbol${"%.2f".format(Locale.US, stat.totalSpent)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Tap a bar for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "$currencySymbol${"%.0f".format(Locale.US, maxVal)}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Text(
                    "$currencySymbol${"%.0f".format(Locale.US, maxVal / 2)}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Text(
                    "0",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Chart Area
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 48.dp) // Space for Y-axis
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / (data.size * 2f)
                            val spacing = barWidth
                            data.forEachIndexed { index, _ ->
                                val x = spacing + index * (barWidth + spacing)
                                if (offset.x >= x && offset.x <= x + barWidth) {
                                    selectedIndex = index
                                }
                            }
                        }
                    }
            ) {
                val barWidth = size.width / (data.size * 2f)
                val spacing = barWidth

                // Draw horizontal grid lines
                val gridColor = primaryColor.copy(alpha = 0.1f)
                drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                
                data.forEachIndexed { index, stat ->
                    val barHeight = (stat.totalSpent / maxVal).toFloat() * size.height
                    val x = spacing + index * (barWidth + spacing)
                    
                    drawRoundRect(
                        color = if (selectedIndex == index) primaryColor else primaryColor.copy(alpha = 0.4f),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { stat ->
                val month = stat.period.split("-").last()
                val monthName = when(month) {
                    "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
                    "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
                    "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
                    else -> month
                }
                Text(monthName, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun CategoryDonutChart(data: List<CategoryStat>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.totalSpent }
    
    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx() // Reduced from 40dp
        // Adjust the drawing area to account for the stroke width and round caps to prevent clipping
        val padding = strokeWidth / 2 + 4.dp.toPx() 
        val chartSize = Size(size.width - padding * 2, size.height - padding * 2)
        val topLeft = Offset(padding, padding)
        
        var startAngle = -90f
        data.forEach { stat ->
            val sweepAngle = (stat.totalSpent / total).toFloat() * 360f
            drawArc(
                color = Color(stat.categoryColor),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = chartSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryLegendItem(stat: CategoryStat, currencySymbol: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(10.dp).background(Color(stat.categoryColor), CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stat.categoryName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$currencySymbol${"%.0f".format(Locale.US, stat.totalSpent)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
