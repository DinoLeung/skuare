package xyz.d1n0.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import xyz.d1n0.ui.navigation.NavGraph

@Composable
fun NavScaffold() {
    val navHostController = rememberNavController()
    val navBackStackEntry by navHostController.currentBackStackEntryAsState()
    val currentBottomNavBarRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination?.route?.let { route ->
                navBarItems
                    .find { it.route.route ==  route }
                    ?.route
            }
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = currentBottomNavBarRoute != null,
                enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
                exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight })
            ) {
                NavBar(
                    items = navBarItems,
                    currentRoute = currentBottomNavBarRoute,
                    onItemClick = { item ->
                        navHostController.navigate(item.route.route) {
                            navHostController.graph.startDestinationRoute?.let {
                                popUpTo(it) { saveState = false }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        NavGraph(
            navHostController = navHostController,
            innerPadding = innerPadding,
        )
    }
}