package xyz.d1n0.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun NavScaffold(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
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
                        navController.navigate(item.route.route) {
                            navController.graph.startDestinationRoute?.let {
                                popUpTo(it) { saveState = true }
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
            navController = navController,
            innerPadding = innerPadding,
        )
    }
}