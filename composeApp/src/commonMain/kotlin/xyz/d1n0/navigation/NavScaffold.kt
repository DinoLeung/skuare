package xyz.d1n0.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun NavBarScaffold(
    rootNavController: NavHostController,
) {
    val navController: NavHostController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(navBackStackEntry) {
        derivedStateOf {
            navBackStackEntry?.destination?.route?.let { route ->
                navItemList
                    .find { it.route.route ==  route }
                    ?.route
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavBar(
                items = navItemList,
                currentRoute = currentRoute,
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
    ) { innerPadding ->
        NavBarGraph(
            navController = navController,
//            innerPadding = innerPadding,
        )
    }
}