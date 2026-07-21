package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.entities.ShoppingItem
import com.corner.myshoppinglist.viewmodel.ListDetailViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    viewModel: ListDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val shoppingList by viewModel.shoppingList.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val estimatedTotal by viewModel.estimatedTotal.collectAsStateWithLifecycle()
    val actualTotal by viewModel.actualTotal.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    
    var showAddItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUncompleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = shoppingList?.name ?: "Loading...",
                        modifier = Modifier.clickable { if (shoppingList != null) showRenameDialog = true }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (shoppingList?.isCompleted == false) {
                        IconButton(onClick = { viewModel.completeList() }) {
                            Icon(Icons.Default.Check, contentDescription = "Complete Shopping")
                        }
                    } else if (shoppingList?.isCompleted == true) {
                        IconButton(onClick = { showUncompleteDialog = true }) {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = "Uncomplete Shopping")
                        }
                    }
                    IconButton(onClick = { /* Photo picker */ }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Add Photo")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename List") },
                            onClick = {
                                showRenameDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear All Items") },
                            onClick = {
                                viewModel.clearAllItems()
                                showMenu = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TotalCostCard(
                estimatedTotal = estimatedTotal,
                actualTotal = actualTotal,
                currencySymbol = currencySymbol,
                isCompleted = shoppingList?.isCompleted ?: false
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteItem(item)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Item deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.addItem(
                                            name = item.itemName,
                                            estimatedPrice = item.estimatedPrice,
                                            quantity = item.quantity,
                                            unit = item.unit,
                                            notes = item.notes
                                        )
                                    }
                                }
                                true
                            } else if (it == SwipeToDismissBoxValue.StartToEnd) {
                                editingItem = item
                                false // Don't actually dismiss
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) 
                                    Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                } else if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                        }
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ShoppingItemRow(
                                item = item,
                                currencySymbol = currencySymbol,
                                onCheckedChange = { checked ->
                                    viewModel.updateItem(item.copy(purchased = checked))
                                },
                                onEdit = { editingItem = item },
                                onDelete = {
                                    viewModel.deleteItem(item)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Item deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addItem(
                                                name = item.itemName,
                                                estimatedPrice = item.estimatedPrice,
                                                quantity = item.quantity,
                                                unit = item.unit,
                                                notes = item.notes
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddItemSheet) {
        AddItemBottomSheet(
            onDismiss = { showAddItemSheet = false },
            onAddItem = { name, price, qty, unit, notes ->
                viewModel.addItem(name, price, qty, unit, notes)
                showAddItemSheet = false
            },
            currencySymbol = currencySymbol,
            viewModel = viewModel
        )
    }

    editingItem?.let { item ->
        ItemEditorDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updatedItem ->
                viewModel.updateItem(updatedItem)
                editingItem = null
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(shoppingList?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename List") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameList(newName)
                        showRenameDialog = false
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUncompleteDialog) {
        AlertDialog(
            onDismissRequest = { showUncompleteDialog = false },
            title = { Text("Uncomplete List?") },
            text = { Text("This will mark the list as active again.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.uncompleteList()
                    showUncompleteDialog = false
                }) {
                    Text("Uncomplete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUncompleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TotalCostCard(
    estimatedTotal: Double,
    actualTotal: Double,
    currencySymbol: String,
    isCompleted: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val total = if (isCompleted) actualTotal else estimatedTotal
            Text(
                text = if (isCompleted) "Total Paid" else "Estimated Total",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$currencySymbol${"%.2f".format(Locale.US, total)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    currencySymbol: String,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.purchased,
            onCheckedChange = onCheckedChange
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.purchased) TextDecoration.LineThrough else null
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item.estimatedPrice?.let {
                    Text(
                        text = "Est: $currencySymbol${"%.2f".format(Locale.US, it)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item.actualPrice?.let {
                    Text(
                        text = "Paid: $currencySymbol${"%.2f".format(Locale.US, it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Text(
            text = "x${item.quantity}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemBottomSheet(
    onDismiss: () -> Unit,
    onAddItem: (String, Double?, Int, String?, String?) -> Unit,
    currencySymbol: String,
    viewModel: ListDetailViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var itemName by remember { mutableStateOf("") }
    var estimatedPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text("Add Item", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = itemName,
                onValueChange = {
                    itemName = it
                    viewModel.searchSuggestions(it)
                },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            if (suggestions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(suggestions) { suggestion ->
                        val priceText = "Last: $currencySymbol${"%.2f".format(Locale.US, suggestion.lastPrice)}"
                        ListItem(
                            headlineContent = { Text(suggestion.name) },
                            supportingContent = { Text(priceText) },
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .offset(x = (-8).dp),
                            trailingContent = {
                                IconButton(onClick = {
                                    onAddItem(suggestion.name, suggestion.lastPrice, 1, null, null)
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Add")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(2f),
                    placeholder = { Text("kg, pack, etc") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = estimatedPrice,
                onValueChange = { estimatedPrice = it },
                label = { Text("Estimated Price ($currencySymbol)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (itemName.isNotBlank()) {
                        onAddItem(
                            itemName, 
                            estimatedPrice.toDoubleOrNull(),
                            quantity.toIntOrNull() ?: 1,
                            unit.ifBlank { null },
                            notes.ifBlank { null }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to List")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
