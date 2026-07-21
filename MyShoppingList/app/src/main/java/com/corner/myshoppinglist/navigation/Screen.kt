package com.corner.myshoppinglist.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ListDetail : Screen("list_detail/{listId}") {
        fun createRoute(listId: Long) = "list_detail/$listId"
    }
    object Settings : Screen("settings")
    object MasterLibrary : Screen("master_library")
}
