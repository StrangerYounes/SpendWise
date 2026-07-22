package com.corner.myshoppinglist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corner.myshoppinglist.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
        }
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
