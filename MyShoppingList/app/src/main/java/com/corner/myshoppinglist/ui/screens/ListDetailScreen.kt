package com.corner.myshoppinglist.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.corner.myshoppinglist.data.local.entities.Photo
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
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val estimatedTotal by viewModel.estimatedTotal.collectAsStateWithLifecycle()
    val actualTotal by viewModel.actualTotal.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val stores by viewModel.stores.collectAsStateWithLifecycle()
    val currentStore by viewModel.currentStore.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.addPhoto(it.toString())
            }
        }
    )
    
    var showAddItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUncompleteDialog by remember { mutableStateOf(false) }
    var showStoreDialog by remember { mutableStateOf(false) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var viewingPhotoUri by remember { mutableStateOf<String?>(null) }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }
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
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
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
            StoreSelector(
                currentStoreName = currentStore?.name,
                onSelectStore = { showStoreDialog = true }
            )

            TotalCostCard(
                estimatedTotal = estimatedTotal,
                actualTotal = actualTotal,
                currencySymbol = currencySymbol,
                isCompleted = shoppingList?.isCompleted ?: false
            )

            if (photos.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        Box {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewingPhotoUri = photo.uri },
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { photoToDelete = photo },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        RoundedCornerShape(bottomStart = 8.dp)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete Photo",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
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
                                },
                                onMoveUp = if (index > 0 && items[index - 1].purchased == item.purchased) {
                                    { viewModel.moveItem(index, index - 1) }
                                } else null,
                                onMoveDown = if (index < items.size - 1 && items[index + 1].purchased == item.purchased) {
                                    { viewModel.moveItem(index, index + 1) }
                                } else null
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
            stores = stores,
            viewModel = viewModel,
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
                    singleLine = true,
                    trailingIcon = {
                        if (newName.isNotEmpty()) {
                            IconButton(onClick = { newName = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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

    if (showStoreDialog) {
        AlertDialog(
            onDismissRequest = { showStoreDialog = false },
            title = { Text("Select Store") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item {
                        ListItem(
                            headlineContent = { Text("No Store (General)") },
                            modifier = Modifier.clickable {
                                viewModel.updateListStore(null)
                                showStoreDialog = false
                            }
                        )
                    }
                    items(stores) { store ->
                        ListItem(
                            headlineContent = { Text(store.name) },
                            modifier = Modifier.clickable {
                                viewModel.updateListStore(store.id)
                                showStoreDialog = false
                            }
                        )
                    }
                    item {
                        TextButton(
                            onClick = { showAddStoreDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add New Store")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStoreDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddStoreDialog) {
        var newStoreName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddStoreDialog = false },
            title = { Text("Add Store") },
            text = {
                OutlinedTextField(
                    value = newStoreName,
                    onValueChange = { newStoreName = it },
                    label = { Text("Store Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newStoreName.isNotBlank()) {
                        viewModel.addStore(newStoreName)
                        showAddStoreDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Photo?") },
            text = { Text("Are you sure you want to remove this photo from the list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePhoto(photo)
                        photoToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (viewingPhotoUri != null) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewingPhotoUri = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = viewingPhotoUri,
                    contentDescription = "View Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { viewingPhotoUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StoreSelector(
    currentStoreName: String?,
    onSelectStore: () -> Unit
) {
    Surface(
        onClick = onSelectStore,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Storefront, contentDescription = null)
            Text(
                text = currentStoreName ?: "Select Store",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
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
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onMoveUp != null || onMoveDown != null) {
            Column(modifier = Modifier.width(32.dp)) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDropUp, contentDescription = "Move Up")
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Move Down")
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }

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
            if (!item.notes.isNullOrBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            text = "x${"%.1f".format(Locale.US, item.quantity)}",
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
    onAddItem: (String, Double?, Double, String?, String?) -> Unit,
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
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (itemName.isNotEmpty()) {
                        IconButton(onClick = { 
                            itemName = ""
                            viewModel.searchSuggestions("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            
            if (suggestions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(suggestions) { suggestion ->
                        val master = suggestion.masterItem
                        val priceToUse = suggestion.listStorePrice ?: suggestion.bestPrice ?: master.lastPrice
                        
                        ListItem(
                            headlineContent = { Text(master.name) },
                            supportingContent = {
                                Column {
                                    Text("Last: $currencySymbol${"%.2f".format(Locale.US, priceToUse)}")
                                    if (suggestion.bestPrice != null && suggestion.bestPrice < (suggestion.listStorePrice ?: Double.MAX_VALUE)) {
                                        Text(
                                            "Cheaper at ${suggestion.bestStoreName}: $currencySymbol${"%.2f".format(Locale.US, suggestion.bestPrice)}",
                                            color = MaterialTheme.colorScheme.tertiary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onAddItem(master.name, priceToUse, 1.0, null, null) }
                                .padding(vertical = 4.dp),
                            trailingContent = {
                                Icon(Icons.Default.Check, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(2f),
                    placeholder = { Text("kg, pack, etc") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (itemName.isNotBlank()) {
                        onAddItem(
                            itemName, 
                            estimatedPrice.toDoubleOrNull(),
                            quantity.toDoubleOrNull() ?: 1.0,
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
