package com.corner.myshoppinglist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.corner.myshoppinglist.ShoppingApplication
import com.corner.myshoppinglist.ui.screens.*
import com.corner.myshoppinglist.viewmodel.*

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as ShoppingApplication
    val repository = app.repository
    val settingsRepository = app.settingsRepository
    
    val receiptViewModel: ReceiptViewModel = viewModel(
        factory = ReceiptViewModelFactory(repository)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            val viewModel: ShoppingViewModel = viewModel(
                factory = ShoppingViewModelFactory(repository, settingsRepository)
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { listId ->
                    navController.navigate(Screen.ListDetail.createRoute(listId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToScanner = {
                    navController.navigate(Screen.ReceiptScanner.route)
                }
            )
        }
        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: -1L
            val viewModel = viewModel<ListDetailViewModel>(
                factory = ListDetailViewModelFactory(repository, settingsRepository, listId)
            )
            ListDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(settingsRepository, app.backupRepository)
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLibrary = { navController.navigate(Screen.MasterLibrary.route) }
            )
        }
        composable(Screen.MasterLibrary.route) {
            val viewModel: MasterItemViewModel = viewModel(
                factory = MasterItemViewModelFactory(repository, settingsRepository)
            )
            MasterLibraryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Stats.route) {
            val viewModel: ShoppingViewModel = viewModel(
                factory = ShoppingViewModelFactory(repository, settingsRepository)
            )
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ReceiptScanner.route) {
            ReceiptScannerScreen(
                viewModel = receiptViewModel,
                onNavigateToReview = {
                    navController.navigate(Screen.ReceiptReview.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ReceiptReview.route) {
            val shoppingViewModel: ShoppingViewModel = viewModel(
                factory = ShoppingViewModelFactory(repository, settingsRepository)
            )
            val shoppingLists by shoppingViewModel.shoppingLists.collectAsStateWithLifecycle()
            
            ReceiptReviewScreen(
                viewModel = receiptViewModel,
                shoppingLists = shoppingLists,
                onNavigateBack = { navController.popBackStack() },
                onFinish = {
                    receiptViewModel.reset()
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
