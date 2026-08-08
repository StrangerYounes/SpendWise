package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.dao.ShoppingListWithDetails
import com.corner.myshoppinglist.data.model.ScannedItem
import com.corner.myshoppinglist.viewmodel.ReceiptUiState
import com.corner.myshoppinglist.viewmodel.ReceiptViewModel
import java.text.NumberFormat
import java.util.Locale

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
    val conversionRate by viewModel.conversionRate.collectAsStateWithLifecycle()
    
    var selectedListId by remember { mutableStateOf<Long?>(null) }
    var showListSelector by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ScannedItem?>(null) }
    var rateInput by remember { mutableStateOf("") }

    LaunchedEffect(conversionRate) {
        if (rateInput.isEmpty() && conversionRate != 1.0) {
            rateInput = if (conversionRate % 1.0 == 0.0) conversionRate.toLong().toString() else conversionRate.toString()
        }
    }

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
                    IconButton(onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    }) {
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
                Column {
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

                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { 
                            // Only allow digits and decimal point
                            if (it.isEmpty() || it.matches(Regex("""^[\d.,]*$"""))) {
                                val cleanValue = it.replace(",", "")
                                rateInput = cleanValue
                                cleanValue.toDoubleOrNull()?.let { rate -> viewModel.setConversionRate(rate) }
                            }
                        },
                        label = { Text("Conversion Rate") },
                        prefix = { Text("1$ = ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )
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
                        onEdit = { itemToEdit = item },
                        onDelete = { viewModel.removeItem(item.id) }
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
        var price by remember { mutableStateOf("%.2f".format(item.totalPrice ?: 0.0)) }
        var quantity by remember { mutableStateOf("%.2f".format(item.quantity)) }

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
                    val rawPrice = price.toDoubleOrNull() ?: 0.0
                    val rawQty = quantity.toDoubleOrNull() ?: 1.0
                    
                    viewModel.updateScannedItem(
                        item.copy(
                            name = name,
                            totalPrice = Math.round(rawPrice * 100.0) / 100.0,
                            quantity = Math.round(rawQty * 100.0) / 100.0
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

class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = originalText.split('.')
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""

        val formattedInteger = NumberFormat.getIntegerInstance(Locale.US).format(integerPart.toLongOrNull() ?: 0L)
        // If the user is typing (e.g. "123"), the formatter works. 
        // If it's empty but has decimal (e.g. ".45"), integerPart is empty.
        val finalInteger = if (integerPart.isEmpty()) "" else formattedInteger
        
        val output = finalInteger + decimalPart
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val originalSub = originalText.substring(0, offset)
                val subParts = originalSub.split('.')
                val subInteger = subParts[0]
                val subDecimal = if (subParts.size > 1) "." + subParts[1] else ""
                
                val formattedSubInteger = if (subInteger.isEmpty()) "" else NumberFormat.getIntegerInstance(Locale.US).format(subInteger.toLongOrNull() ?: 0L)
                return formattedSubInteger.length + subDecimal.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val transformedSub = output.substring(0, offset.coerceAtMost(output.length))
                return transformedSub.replace(",", "").length
            }
        }

        return TransformedText(androidx.compose.ui.text.AnnotatedString(output), offsetMapping)
    }
}

@Composable
fun ScannedItemRow(
    item: ScannedItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val unitPrice = if (item.quantity > 0) (item.totalPrice ?: 0.0) / item.quantity else 0.0
    val numberFormat = remember { 
        NumberFormat.getInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

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
                    "${"%.2f".format(item.quantity)} ${item.unit ?: "pc"} • Total: ${numberFormat.format(item.totalPrice ?: 0.0)} (Unit: ${numberFormat.format(unitPrice)})",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
