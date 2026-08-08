package com.corner.myshoppinglist.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.viewmodel.BackupStatus
import com.corner.myshoppinglist.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingBackupData by remember { mutableStateOf<String?>(null) }
    var showExportOptions by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            pendingBackupData?.let { data ->
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(data.toByteArray())
                }
                pendingBackupData = null
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                val jsonData = inputStream.bufferedReader().use { reader -> reader.readText() }
                viewModel.restoreData(jsonData)
            }
        }
    }

    LaunchedEffect(backupStatus) {
        when (val status = backupStatus) {
            is BackupStatus.Success -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.resetBackupStatus()
            }
            is BackupStatus.Error -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.resetBackupStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("General", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            var showCurrencyDialog by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("Currency") },
                supportingContent = { Text(settings?.currencySymbol ?: "$") },
                modifier = Modifier.clickable { showCurrencyDialog = true }
            )
            
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { 
                    val themeText = when(settings?.darkTheme) {
                        true -> "Dark"
                        false -> "Light"
                        else -> "Follow System"
                    }
                    Text(themeText) 
                },
                modifier = Modifier.clickable { 
                    // Simple cycle for this demo: System -> Light -> Dark -> System
                    val next = when(settings?.darkTheme) {
                        null -> false
                        false -> true
                        else -> null
                    }
                    viewModel.toggleDarkTheme(next)
                }
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Data", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            ListItem(
                headlineContent = { Text("Smart Item Library") },
                supportingContent = { Text("View and edit all items ever added") },
                modifier = Modifier.clickable { onNavigateToLibrary() }
            )

            ListItem(
                headlineContent = { Text("Backup Data") },
                supportingContent = { Text("Export your items and lists to a file") },
                modifier = Modifier.clickable { showExportOptions = true }
            )

            ListItem(
                headlineContent = { Text("Restore Data") },
                supportingContent = { Text("Import data from a previously exported file") },
                modifier = Modifier.clickable { 
                    openDocumentLauncher.launch(arrayOf("application/json"))
                }
            )
            
            if (showExportOptions) {
                AlertDialog(
                    onDismissRequest = { showExportOptions = false },
                    title = { Text("Backup Data") },
                    text = {
                        Column {
                            Text("Choose what to export:")
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                viewModel.exportData(itemsOnly = true) { data ->
                                    pendingBackupData = data
                                    createDocumentLauncher.launch("shopping_items_backup.json")
                                }
                                showExportOptions = false
                            }) {
                                Text("Smart Item Library Only")
                            }
                            TextButton(onClick = {
                                viewModel.exportData(itemsOnly = false) { data ->
                                    pendingBackupData = data
                                    createDocumentLauncher.launch("full_shopping_backup.json")
                                }
                                showExportOptions = false
                            }) {
                                Text("All Data (Items, Lists, Categories, Stores)")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showExportOptions = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (backupStatus is BackupStatus.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
            
            if (showCurrencyDialog) {
                val currencies = listOf("$", "€", "£", "¥", "Rp")
                AlertDialog(
                    onDismissRequest = { showCurrencyDialog = false },
                    title = { Text("Select Currency") },
                    text = {
                        Column {
                            currencies.forEach { symbol ->
                                ListItem(
                                    headlineContent = { Text(symbol) },
                                    modifier = Modifier.clickable {
                                        viewModel.setCurrency(symbol)
                                        showCurrencyDialog = false
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCurrencyDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
