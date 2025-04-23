package xyz.d1n0.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Alarm
import androidx.compose.material.icons.sharp.Schedule
import androidx.compose.material.icons.sharp.TaskAlt
import androidx.compose.material.icons.sharp.Timer
import androidx.compose.material.icons.sharp.Tune
import androidx.compose.ui.graphics.vector.ImageVector

data class NavBarItem(
	val title: String,
	val route: NavBarRoute,
	val icon: ImageVector,
)

val navBarItems = listOf(
	NavBarItem(
		title = "Clocks",
		route = NavBarRoute.Clocks,
		icon = Icons.Sharp.Schedule,
	), NavBarItem(
		title = "Alarms",
		route = NavBarRoute.Alarms,
		icon = Icons.Sharp.Alarm,
	), NavBarItem(
		title = "Timer",
		route = NavBarRoute.Timer,
		icon = Icons.Sharp.Timer,
	), NavBarItem(
		title = "Reminders",
		route = NavBarRoute.Reminders,
		icon = Icons.Sharp.TaskAlt,
	), NavBarItem(
		title = "Settings",
		route = NavBarRoute.Settings,
		icon = Icons.Sharp.Tune,
	)

)