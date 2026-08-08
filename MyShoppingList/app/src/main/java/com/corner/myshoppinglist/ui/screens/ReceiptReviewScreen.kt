package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.dao.ShoppingListWithDetails
import com.corner.myshoppinglist.data.model.ScannedItem
import com.corner.myshoppinglist.viewmodel.ReceiptUiState
import com.corner.myshoppinglist.viewmodel.ReceiptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    viewModel: ReceiptViewModel,
    shoppingLists: List<ShoppingListWithDetails>,
    onNavigateBack: () -> Unit,
    onFinish: () -> Unit
) {
    val items by viewModel.scannedItems.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedListId by remember { mutableStateOf<Long?>(null) }
    var showListSelector by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ScannedItem?>(null) }

    // Default to the most recent active list if available
    LaunchedEffect(shoppingLists) {
        if (selectedListId == null && shoppingLists.isNotEmpty()) {
            selectedListId = shoppingLists.firstOrNull { !it.shoppingList.isCompleted }?.shoppingList?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Scanned Items") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedListId != null) {
                        IconButton(
                            onClick = { viewModel.saveItemsToList(selectedListId!!, onFinish) },
                            enabled = uiState !is ReceiptUiState.Saving
                        ) {
                            if (uiState is ReceiptUiState.Saving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Done")
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // List Selector Header
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { showListSelector = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Add to List:", style = MaterialTheme.typography.labelMedium)
                        val selectedList = shoppingLists.find { it.shoppingList.id == selectedListId }
                        Text(
                            selectedList?.shoppingList?.name ?: "Select a list",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { showListSelector = true }) {
                        Text("Change")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ScannedItemRow(
                        item = item,
                        onToggle = { viewModel.toggleItemSelected(item.id) },
                        onEdit = { itemToEdit = item }
                    )
                }
            }
        }
    }

    if (showListSelector) {
        AlertDialog(
            onDismissRequest = { showListSelector = false },
            title = { Text("Select Shopping List") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(shoppingLists.filter { !it.shoppingList.isCompleted }) { list ->
                        ListItem(
                            headlineContent = { Text(list.shoppingList.name) },
                            modifier = Modifier.clickable {
                                selectedListId = list.shoppingList.id
                                showListSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showListSelector = false }) { Text("Cancel") }
            }
        )
    }

    itemToEdit?.let { item ->
        var name by remember { mutableStateOf(item.name) }
        var price by remember { mutableStateOf(item.totalPrice?.toString() ?: "") }
        var quantity by remember { mutableStateOf(item.quantity.toString()) }

        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Edit Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    TextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Total Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    TextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateScannedItem(
                        item.copy(
                            name = name,
                            totalPrice = price.toDoubleOrNull(),
                            quantity = quantity.toDoubleOrNull() ?: 1.0
                        )
                    )
                    itemToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ScannedItemRow(
    item: ScannedItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isSelected, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Medium)
                Text(
                    "${item.quantity} ${item.unit ?: "pc"} • ${item.totalPrice ?: "?.??"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
        }
    }
}
