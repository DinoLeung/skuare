package xyz.d1n0.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.ui.theme.SkuareTheme

@Composable
fun NavBar(
	items: List<NavBarItem>,
	currentRoute: NavBarRoute?,
	unsavedRoutes: Set<NavBarRoute> = emptySet(),
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
					BadgedBox(
						badge = { if (it.route in unsavedRoutes) Badge() },
					) {
						Icon(
							imageVector = it.icon,
							contentDescription = it.title,
						)
					}
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

@Preview
@Composable
private fun NavBarPreview() {
	SkuareTheme {
		NavBar(
			items = navBarItems,
			currentRoute = navBarItems[2].route,
			unsavedRoutes = setOf(navBarItems[0].route, navBarItems[4].route),
			onItemClick = {}
		)
	}
}
