package xyz.d1n0.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Alarm
import androidx.compose.material.icons.sharp.Schedule
import androidx.compose.material.icons.sharp.TaskAlt
import androidx.compose.material.icons.sharp.Timer
import androidx.compose.material.icons.sharp.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import xyz.d1n0.lib.model.WatchFeatures
import xyz.d1n0.resources.*

data class NavBarItem(
	val title: StringResource,
	val route: NavBarRoute,
	val icon: ImageVector,
)

val navBarItems = listOf(
	NavBarItem(
		title = Res.string.nav_timezones,
		route = NavBarRoute.Timezones,
		icon = Icons.Sharp.Schedule,
	), NavBarItem(
		title = Res.string.nav_alarms,
		route = NavBarRoute.Alarms,
		icon = Icons.Sharp.Alarm,
	), NavBarItem(
		title = Res.string.nav_timer,
		route = NavBarRoute.Timer,
		icon = Icons.Sharp.Timer,
	), NavBarItem(
		title = Res.string.nav_reminders,
		route = NavBarRoute.Reminders,
		icon = Icons.Sharp.TaskAlt,
	), NavBarItem(
		title = Res.string.nav_settings,
		route = NavBarRoute.Settings,
		icon = Icons.Sharp.Tune,
	)

)

/**
 * [navBarItems] without the tabs for features [model] doesn't have, e.g. reminders.
 */
fun navBarItemsFor(model: WatchFeatures): List<NavBarItem> = navBarItems.filter {
	it.route != NavBarRoute.Reminders || model.reminderCount > 0
}