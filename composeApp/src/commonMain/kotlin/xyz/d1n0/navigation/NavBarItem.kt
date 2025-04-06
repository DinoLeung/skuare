package xyz.d1n0.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class NavBarItem (
    val title: String,
    val route: NavBarRoute,
    val icon: ImageVector,
)