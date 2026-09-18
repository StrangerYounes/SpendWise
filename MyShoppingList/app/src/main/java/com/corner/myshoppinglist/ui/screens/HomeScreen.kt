package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.data.local.dao.ShoppingListWithDetails
import com.corner.myshoppinglist.viewmodel.ShoppingViewModel
import android.graphics.Color as AndroidColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ShoppingViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val shoppingLists by viewModel.shoppingLists.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchedItems by viewModel.searchedItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var listToEdit by remember { mutableStateOf<com.corner.myshoppinglist.data.local.entities.ShoppingList?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val activeLists = remember(shoppingLists) { shoppingLists.filter { !it.shoppingList.isCompleted } }
    val completedLists = remember(shoppingLists) { shoppingLists.filter { it.shoppingList.isCompleted } }
    
    val tabs = listOf(
        "Active (${activeLists.size})",
        "Completed (${completedLists.size})"
    )

    val currentDisplayLists = if (selectedTabIndex == 0) activeLists else completedLists
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("SpendWise") },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Receipt")
                    }
                    IconButton(onClick = { showCategoryDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Categories")
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistics")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior
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
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {}

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
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
                            supportingContent = {
                                Column {
                                    if (!item.notes.isNullOrBlank()) {
                                        Text(
                                            text = item.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                    Text("In list: #${item.listId}")
                                }
                            },
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

                if (currentDisplayLists.isNotEmpty()) {
                    if (searchQuery.isNotEmpty()) {
                        item {
                            Text(
                                "Shopping Lists",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(currentDisplayLists, key = { "list_${it.shoppingList.id}" }) { item ->
                        ShoppingListCard(
                            item = item,
                            currencySymbol = currencySymbol,
                            onClick = { onNavigateToDetail(item.shoppingList.id) },
                            onDelete = { viewModel.deleteShoppingList(item.shoppingList) },
                            onEdit = { listToEdit = item.shoppingList }
                        )
                    }
                } else if (searchQuery.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (selectedTabIndex == 0) "No active shopping lists." else "No completed lists yet.")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var listName by remember { mutableStateOf("") }
        var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Shopping List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = listName,
                        onValueChange = { listName = it },
                        placeholder = { Text("Enter list name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (listName.isNotEmpty()) {
                                IconButton(onClick = { listName = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Box {
                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedCategory = categories.find { it.id == selectedCategoryId }
                                Text(selectedCategory?.name ?: "Groceries (Default)")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Groceries (Default)") },
                                onClick = {
                                    selectedCategoryId = null
                                    expanded = false
                                }
                            )
                            categories.filter { it.name != "Groceries" }.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (listName.isNotBlank()) {
                            viewModel.addShoppingList(listName, selectedCategoryId)
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

    listToEdit?.let { list ->
        var newName by remember { mutableStateOf(list.name) }
        var selectedCategoryId by remember { mutableStateOf(list.categoryId) }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { listToEdit = null },
            title = { Text("Edit Shopping List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Enter list name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (newName.isNotEmpty()) {
                                IconButton(onClick = { newName = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    Box {
                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedCategory = categories.find { it.id == selectedCategoryId }
                                Text(selectedCategory?.name ?: "Groceries (Default)")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Groceries (Default)") },
                                onClick = {
                                    selectedCategoryId = null
                                    expanded = false
                                }
                            )
                            categories.filter { it.name != "Groceries" }.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.updateShoppingList(list.copy(name = newName, categoryId = selectedCategoryId))
                            listToEdit = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        var editingCategory by remember { mutableStateOf<com.corner.myshoppinglist.data.local.entities.Category?>(null) }
        var selectedColor by remember { mutableStateOf(0xFF4CAF50.toInt()) } // Default green

        AlertDialog(
            onDismissRequest = { 
                showCategoryDialog = false
                editingCategory = null
                categoryName = ""
                selectedColor = 0xFF4CAF50.toInt()
            },
            title = { Text(if (editingCategory == null) "Manage Categories" else "Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (editingCategory == null) {
                        Text("Existing Categories", style = MaterialTheme.typography.titleSmall)
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(categories) { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingCategory = category
                                            categoryName = category.name
                                            selectedColor = category.color
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(16.dp)
                                                .background(Color(category.color), MaterialTheme.shapes.extraSmall)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.name)
                                    }
                                    if (category.name != "Groceries") {
                                        IconButton(onClick = { viewModel.deleteCategory(category) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    Text(
                        if (editingCategory == null) "Add New Category" else "Edit Category Settings", 
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        placeholder = { Text("Category name") },
                        singleLine = true,
                        enabled = editingCategory?.name != "Groceries",
                        trailingIcon = {
                            if (categoryName.isNotEmpty() && editingCategory?.name != "Groceries") {
                                IconButton(onClick = { categoryName = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    
                    Text("Select Color", style = MaterialTheme.typography.labelSmall)
                    
                    HsvColorPicker(
                        initialColor = selectedColor,
                        onColorChanged = { selectedColor = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            val currentEditing = editingCategory
                            if (currentEditing != null) {
                                viewModel.updateCategory(currentEditing.copy(name = categoryName, color = selectedColor))
                                editingCategory = null
                                categoryName = ""
                                selectedColor = 0xFF4CAF50.toInt()
                            } else {
                                viewModel.addCategory(categoryName, selectedColor)
                                categoryName = ""
                                selectedColor = 0xFF4CAF50.toInt()
                            }
                        }
                    }
                ) {
                    Text(if (editingCategory == null) "Add" else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (editingCategory != null) {
                            editingCategory = null
                            categoryName = ""
                            selectedColor = 0xFF4CAF50.toInt()
                        } else {
                            showCategoryDialog = false 
                        }
                    }
                ) {
                    Text(if (editingCategory == null) "Close" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun HsvColorPicker(
    initialColor: Int,
    onColorChanged: (Int) -> Unit
) {
    val hsv = remember(initialColor) {
        val res = FloatArray(3)
        AndroidColor.colorToHSV(initialColor, res)
        res
    }

    var hue by remember(initialColor) { mutableStateOf(hsv[0]) }
    var saturation by remember(initialColor) { mutableStateOf(hsv[1]) }
    var value by remember(initialColor) { mutableStateOf(hsv[2]) }

    val currentColor = remember(hue, saturation, value) {
        AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    }

    LaunchedEffect(currentColor) {
        onColorChanged(currentColor)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Saturation-Value Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(hue) {
                        detectTapGestures { offset ->
                            saturation = (offset.x / size.width).coerceIn(0f, 1f)
                            value = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                            value = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        }
                    }
            ) {
                // Hue base
                drawRect(Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))))

                // Saturation gradient (White to Transparent)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )

                // Value gradient (Transparent to Black)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )

                // Selection cursor
                val cursorX = saturation * size.width
                val cursorY = (1f - value) * size.height
                drawCircle(
                    color = if (value > 0.5f) Color.Black else Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(cursorX, cursorY),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(cursorX, cursorY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Hue Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            hue = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                        }
                    }
            ) {
                val hueColors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
                drawRect(brush = Brush.horizontalGradient(hueColors))

                // Cursor
                val cursorX = (hue / 360f) * size.width
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cursorX - 2.dp.toPx(), 0f),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Preview and Hex Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(currentColor))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            )
            
            var hexString by remember(currentColor) {
                mutableStateOf(String.format("%06X", (0xFFFFFF and currentColor)))
            }

            OutlinedTextField(
                value = hexString,
                onValueChange = {
                    hexString = it.uppercase().filter { c -> c in "0123456789ABCDEF" }.take(6)
                    if (hexString.length == 6) {
                        val parsed = AndroidColor.parseColor("#$hexString")
                        val newHsv = FloatArray(3)
                        AndroidColor.colorToHSV(parsed, newHsv)
                        hue = newHsv[0]
                        saturation = newHsv[1]
                        value = newHsv[2]
                    }
                },
                label = { Text("HEX") },
                prefix = { Text("#") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListCard(
    item: ShoppingListWithDetails,
    currencySymbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.shoppingList.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val categoryName = item.category?.name ?: "Groceries"
                        val categoryColor = item.category?.color?.let { Color(it) } 
                            ?: Color(0xFF4CAF50)
                        
                        Surface(
                            color = categoryColor.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.extraSmall,
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = categoryColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateFormat.format(Date(if (item.shoppingList.isCompleted && item.shoppingList.completedDate != null) item.shoppingList.completedDate else item.shoppingList.createdDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        item.storeName?.let {
                            Text(
                                text = " • $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEdit()
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.itemCount} items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.shoppingList.isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                val totalToShow = if (item.shoppingList.isCompleted) item.actualTotal else item.estimatedTotal
                val label = if (item.shoppingList.isCompleted) "Total Spent: " else "Est. Total: "
                
                Text(
                    text = "$label$currencySymbol${"%.2f".format(Locale.US, totalToShow)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.shoppingList.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
