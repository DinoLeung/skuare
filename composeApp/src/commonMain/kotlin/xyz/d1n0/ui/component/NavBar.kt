package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import xyz.d1n0.ui.navigation.NavBarItem
import xyz.d1n0.ui.navigation.NavBarRoute

@Composable
fun NavBar(
	items: List<NavBarItem>,
	currentRoute: NavBarRoute?,
	onItemClick: (NavBarItem) -> Unit,
) {
	NavigationBar(
		modifier = Modifier.fillMaxWidth(),
	) {
		items.forEach {
			NavigationBarItem(
				selected = currentRoute == it.route,
				onClick = { onItemClick(it) },
				icon = {
					Icon(
						imageVector = it.icon,
						contentDescription = it.title,
					)
				},
				label = {
					Text(
						text = it.title, maxLines = 1, overflow = TextOverflow.Ellipsis
					)
				},
			)
		}
	}
}