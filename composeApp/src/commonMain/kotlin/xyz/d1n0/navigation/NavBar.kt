package xyz.d1n0.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

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
//                        imageVector = if (currentRoute == it.route) it.iconActive else it.iconInactive,
                        imageVector = it.icon,
                        contentDescription = it.title,
                    )
                },
                label = {
                    Text(
                        text = it.title,
//                        style = if (currentRoute == it.route) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
            )
        }
    }

}