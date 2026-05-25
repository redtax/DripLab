package com.driplab.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Recipe : Screen("recipe", "配方", Icons.AutoMirrored.Filled.LibraryBooks)
    data object Brew : Screen("brew", "冲煮", Icons.Default.Timer)
    data object Note : Screen("note", "笔记", Icons.Default.Coffee)
    data object Settings : Screen("settings", "我的", Icons.Default.Person)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Recipe,
    Screen.Brew,
    Screen.Note,
    Screen.Settings
)