package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.entities.MasterItem
import com.corner.myshoppinglist.viewmodel.MasterItemViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterLibraryScreen(
    viewModel: MasterItemViewModel,
    onNavigateBack: () -> Unit
) {
    val masterItems by viewModel.masterItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
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
                        }
                    )
                }
            }
        }
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
                    label = { Text("Item Name") }
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price ($currencySymbol)") }
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
                    label = { Text("Item Name") }
                )
                OutlinedTextField(
                    value = lastPrice,
                    onValueChange = { lastPrice = it },
                    label = { Text("Last Price ($currencySymbol)") }
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
