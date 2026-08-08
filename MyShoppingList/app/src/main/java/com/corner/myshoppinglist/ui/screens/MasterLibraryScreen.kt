package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.entities.MasterItem
import com.corner.myshoppinglist.viewmodel.MasterItemDetail
import com.corner.myshoppinglist.viewmodel.MasterItemViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterLibraryScreen(
    viewModel: MasterItemViewModel,
    onNavigateBack: () -> Unit
) {
    val masterItems by viewModel.masterItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val itemDetail by viewModel.itemDetail.collectAsStateWithLifecycle()
    
    var editingItem by remember { mutableStateOf<MasterItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Item Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add New Item")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {}

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(masterItems, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = {
                            Text("Last: $currencySymbol${"%.2f".format(Locale.US, item.lastPrice)} | Avg: $currencySymbol${"%.2f".format(Locale.US, item.averagePrice)}")
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editingItem = item }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteMasterItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                        modifier = Modifier.clickable { viewModel.selectItem(item.id) }
                    )
                }
            }
        }
    }

    itemDetail?.let { detail ->
        ItemDetailDialog(
            detail = detail,
            currencySymbol = currencySymbol,
            viewModel = viewModel,
            onDismiss = { viewModel.selectItem(null) },
            onMerge = { targetItem ->
                viewModel.mergeItems(detail.item, targetItem)
            }
        )
    }

    editingItem?.let { item ->
        MasterItemEditDialog(
            item = item,
            currencySymbol = currencySymbol,
            onDismiss = { editingItem = null },
            onSave = { updatedItem ->
                viewModel.updateMasterItem(updatedItem)
                editingItem = null
            }
        )
    }

    if (showAddDialog) {
        MasterItemAddDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showAddDialog = false },
            onSave = { name, price ->
                viewModel.addMasterItem(name, price)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ItemDetailDialog(
    detail: MasterItemDetail,
    currencySymbol: String,
    viewModel: MasterItemViewModel,
    onDismiss: () -> Unit,
    onMerge: (MasterItem) -> Unit
) {
    val masterItems by viewModel.allMasterItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showMoreHistory by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detail.item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showMergeDialog = true }) {
                        Icon(Icons.Default.CallMerge, contentDescription = "Merge into another item")
                    }
                }

                Text("Price by Store", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (detail.storePrices.isEmpty()) {
                        // Fallback to global last price if no store-specific prices exist
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Global Library Price", fontStyle = FontStyle.Italic)
                            Text(
                                "$currencySymbol${"%.2f".format(Locale.US, detail.item.lastPrice)}",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        detail.storePrices.forEach { priceDetail ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(priceDetail.storeName)
                                Text(
                                    "$currencySymbol${"%.2f".format(Locale.US, priceDetail.storeItemPrice.lastPrice)}",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("Purchase History", style = MaterialTheme.typography.titleMedium)
                if (detail.purchaseHistory.isEmpty()) {
                    Text("No purchase history yet.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                } else {
                    val displayedHistory = if (showMoreHistory) detail.purchaseHistory else detail.purchaseHistory.take(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayedHistory.forEach { history ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        dateFormat.format(Date(history.listDate)),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "$currencySymbol${"%.2f".format(Locale.US, history.shoppingItem.actualPrice ?: history.shoppingItem.estimatedPrice ?: 0.0)}",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${history.storeName ?: "General Store"} - ${history.listName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!showMoreHistory && detail.purchaseHistory.size > 3) {
                            TextButton(
                                onClick = { showMoreHistory = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Show More")
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }

    if (showMergeDialog) {
        MergeSelectionDialog(
            currentItem = detail.item,
            allMasterItems = masterItems,
            onDismiss = { showMergeDialog = false },
            onMerge = { target ->
                onMerge(target)
                showMergeDialog = false
            }
        )
    }
}

@Composable
fun MergeSelectionDialog(
    currentItem: MasterItem,
    allMasterItems: List<MasterItem>,
    onDismiss: () -> Unit,
    onMerge: (MasterItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(searchQuery, allMasterItems) {
        allMasterItems.filter { 
            it.id != currentItem.id && it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge '${currentItem.name}' into...") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Select the item that will remain. All history and prices from '${currentItem.name}' will be moved to it.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search target item...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filteredItems) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            modifier = Modifier.clickable { onMerge(item) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MasterItemAddDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item to Library") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(name, price.toDoubleOrNull() ?: 0.0)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MasterItemEditDialog(
    item: MasterItem,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (MasterItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var lastPrice by remember { mutableStateOf(item.lastPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Library Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    value = lastPrice,
                    onValueChange = { lastPrice = it },
                    label = { Text("Last Price ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(item.copy(name = name, lastPrice = lastPrice.toDoubleOrNull() ?: item.lastPrice))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
