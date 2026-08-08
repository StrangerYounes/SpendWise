package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.entities.ShoppingItem
import com.corner.myshoppinglist.data.local.entities.Store
import com.corner.myshoppinglist.viewmodel.ListDetailViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorDialog(
    item: ShoppingItem,
    stores: List<Store>,
    viewModel: ListDetailViewModel,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    var name by remember { mutableStateOf(item.itemName) }
    var notes by remember { mutableStateOf(item.notes ?: "") }
    var estPrice by remember { mutableStateOf(item.estimatedPrice?.toString() ?: "") }
    var actPrice by remember { mutableStateOf(item.actualPrice?.toString() ?: "") }
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit ?: "") }
    var storeId by remember { mutableStateOf(item.storeId) }
    
    var showStoreDropdown by remember { mutableStateOf(false) }
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Edit Item",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            viewModel.searchSuggestions(it)
                        },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                IconButton(onClick = { 
                                    name = ""
                                    viewModel.searchSuggestions("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }

                if (suggestions.isNotEmpty()) {
                    items(suggestions) { suggestion ->
                        val master = suggestion.masterItem
                        val priceToUse = suggestion.listStorePrice ?: suggestion.bestPrice ?: master.lastPrice
                        
                        ListItem(
                            headlineContent = { Text(master.name) },
                            supportingContent = {
                                Text("Last: $currencySymbol${"%.2f".format(Locale.US, priceToUse)}")
                            },
                            modifier = Modifier.fillMaxWidth()
                                .clickable { 
                                    name = master.name
                                    estPrice = priceToUse.toString()
                                    viewModel.searchSuggestions("")
                                }
                                .padding(vertical = 4.dp),
                            trailingContent = {
                                Icon(Icons.Default.Check, contentDescription = "Select", tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }
                
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (e.g. kg)") },
                            modifier = Modifier.weight(2f),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = estPrice,
                            onValueChange = { estPrice = it },
                            label = { Text("Est. Price") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = actPrice,
                            onValueChange = { actPrice = it },
                            label = { Text("Paid Price") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = showStoreDropdown,
                        onExpandedChange = { showStoreDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = stores.find { it.id == storeId }?.name ?: "Default Store",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Item Store Override") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStoreDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showStoreDropdown,
                            onDismissRequest = { showStoreDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default (List Store)") },
                                onClick = {
                                    storeId = null
                                    showStoreDropdown = false
                                }
                            )
                            stores.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text(store.name) },
                                    onClick = {
                                        storeId = store.id
                                        showStoreDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            onSave(
                                item.copy(
                                    itemName = name,
                                    notes = notes.ifBlank { null },
                                    estimatedPrice = estPrice.toDoubleOrNull(),
                                    actualPrice = actPrice.toDoubleOrNull(),
                                    quantity = quantity.toDoubleOrNull() ?: 1.0,
                                    unit = unit.ifBlank { null },
                                    storeId = storeId
                                )
                            )
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

