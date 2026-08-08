package com.corner.myshoppinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.corner.myshoppinglist.navigation.NavGraph
import com.corner.myshoppinglist.ui.theme.MyShoppingListTheme
import com.corner.myshoppinglist.viewmodel.SettingsViewModel
import com.corner.myshoppinglist.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as ShoppingApplication
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(app.settingsRepository, app.backupRepository)
            )
            val settings by settingsViewModel.settings.collectAsState()
            
            val isDark = when(settings?.darkTheme) {
                true -> true
                false -> false
                else -> isSystemInDarkTheme()
            }

            MyShoppingListTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
