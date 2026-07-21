package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.dao.ShoppingListWithDetails
import com.corner.myshoppinglist.viewmodel.ShoppingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ShoppingViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val shoppingLists by viewModel.shoppingLists.collectAsStateWithLifecycle()
    val searchedItems by viewModel.searchedItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var listToRename by remember { mutableStateOf<com.corner.myshoppinglist.data.local.entities.ShoppingList?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("My Shopping Lists") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Shopping List")
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
                placeholder = { Text("Search lists or items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {}

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isNotEmpty() && searchedItems.isNotEmpty()) {
                    item {
                        Text(
                            "Items",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(searchedItems, key = { "item_${it.id}" }) { item ->
                        ListItem(
                            headlineContent = { Text(item.itemName) },
                            supportingContent = { Text("In list: #${item.listId}") },
                            trailingContent = {
                                if (item.purchased) Icon(Icons.Default.Check, contentDescription = null)
                            },
                            modifier = Modifier.clickable { onNavigateToDetail(item.listId) }
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                if (shoppingLists.isNotEmpty()) {
                    if (searchQuery.isNotEmpty()) {
                        item {
                            Text(
                                "Shopping Lists",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(shoppingLists, key = { "list_${it.shoppingList.id}" }) { item ->
                        ShoppingListCard(
                            item = item,
                            currencySymbol = currencySymbol,
                            onClick = { onNavigateToDetail(item.shoppingList.id) },
                            onDelete = { viewModel.deleteShoppingList(item.shoppingList) },
                            onDuplicate = { viewModel.duplicateList(item.shoppingList) },
                            onRename = { listToRename = item.shoppingList }
                        )
                    }
                } else if (searchQuery.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No shopping lists yet. Tap + to create one.")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var listName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Shopping List") },
            text = {
                TextField(
                    value = listName,
                    onValueChange = { listName = it },
                    placeholder = { Text("Enter list name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (listName.isNotBlank()) {
                            viewModel.addShoppingList(listName)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    listToRename?.let { list ->
        var newName by remember { mutableStateOf(list.name) }
        AlertDialog(
            onDismissRequest = { listToRename = null },
            title = { Text("Rename Shopping List") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Enter new name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameShoppingList(list, newName)
                            listToRename = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListCard(
    item: ShoppingListWithDetails,
    currencySymbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.shoppingList.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.shoppingList.isCompleted) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Completed") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(item.shoppingList.createdDate)),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.itemCount} items")
                    Text(
                        text = "Est. Total: $currencySymbol${"%.2f".format(Locale.US, item.estimatedTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            onRename()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            onDuplicate()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
