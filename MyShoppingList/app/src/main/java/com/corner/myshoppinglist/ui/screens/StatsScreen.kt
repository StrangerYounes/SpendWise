package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.dao.CategoryStat
import com.corner.myshoppinglist.data.local.dao.PeriodStat
import com.corner.myshoppinglist.viewmodel.ShoppingViewModel
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
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val totalSpentAllTime = spentPerMonth.sumOf { it.totalSpent }
    val avgSpentPerMonth = if (spentPerMonth.isNotEmpty()) totalSpentAllTime / spentPerMonth.size else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights & Stats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // KPI Summary Cards
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        KPICard(
                            label = "Total Spent",
                            value = "$currencySymbol${"%.0f".format(Locale.US, totalSpentAllTime)}",
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
                                primaryColor = MaterialTheme.colorScheme.primary
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
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                spentByCategory.take(5).forEach { stat ->
                                    CategoryLegendItem(stat, currencySymbol)
                                }
                            }
                        }
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
fun SpendingBarChart(data: List<PeriodStat>, primaryColor: Color) {
    val maxVal = data.maxOfOrNull { it.totalSpent } ?: 1.0
    
    Box(modifier = Modifier.height(200.dp).fillMaxWidth().padding(top = 16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / (data.size * 2f)
            val spacing = barWidth
            
            data.forEachIndexed { index, stat ->
                val barHeight = (stat.totalSpent / maxVal).toFloat() * size.height
                val x = spacing + index * (barWidth + spacing)
                
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
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

@Composable
fun CategoryDonutChart(data: List<CategoryStat>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.totalSpent }
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        data.forEach { stat ->
            val sweepAngle = (stat.totalSpent / total).toFloat() * 360f
            drawArc(
                color = Color(stat.categoryColor),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryLegendItem(stat: CategoryStat, currencySymbol: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(Color(stat.categoryColor), CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stat.categoryName, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(80.dp))
        Text(
            "$currencySymbol${"%.0f".format(Locale.US, stat.totalSpent)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
