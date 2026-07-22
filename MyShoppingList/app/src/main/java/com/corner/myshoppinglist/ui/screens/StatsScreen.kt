package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping Statistics") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Spending per Month", style = MaterialTheme.typography.titleLarge)
            }
            items(spentPerMonth) { stat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.period)
                        Text(
                            "$currencySymbol${"%.2f".format(Locale.US, stat.totalSpent)}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Text("Most Expensive Purchases", style = MaterialTheme.typography.titleLarge)
            }
            items(expensiveItems) { item ->
                ListItem(
                    headlineContent = { Text(item.itemName) },
                    trailingContent = {
                        Text("$currencySymbol${"%.2f".format(Locale.US, item.maxPrice)}")
                    }
                )
            }

            item {
                Text("Spending by Item", style = MaterialTheme.typography.titleLarge)
            }
            items(itemStats) { stat ->
                ListItem(
                    headlineContent = { Text(stat.itemName) },
                    supportingContent = { Text("Quantity: ${"%.1f".format(Locale.US, stat.totalQuantity)}") },
                    trailingContent = {
                        Text("$currencySymbol${"%.2f".format(Locale.US, stat.totalSpent)}")
                    }
                )
            }
        }
    }
}
