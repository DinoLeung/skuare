package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.data.database.PressedLocation
import xyz.d1n0.data.database.PressedLocationWithWatch
import xyz.d1n0.resources.*
import xyz.d1n0.ui.text.LocalLocaleFormatter
import xyz.d1n0.ui.text.formatCoordinate
import xyz.d1n0.ui.text.formatDateTime
import xyz.d1n0.ui.theme.SkuareTheme
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * A recorded location: its name, the time of the press on the watch's lower-right button until the
 * user renames it, the watch, and where the phone was. A long press opens a menu with Rename and
 * Delete.
 *
 * @param onClick Opens the location in the phone's map app.
 */
@Composable
fun LocationCard(
	item: PressedLocationWithWatch,
	onClick: () -> Unit,
	onRename: () -> Unit,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier,
) {
	var menuExpanded by remember { mutableStateOf(false) }
	// the box anchors the menu to the card
	Box(modifier = modifier) {
		LocationCardContent(
			item = item,
			onClick = onClick,
			onLongClick = { menuExpanded = true },
			modifier = Modifier.fillMaxWidth(),
		)
		DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
			DropdownMenuItem(
				text = { Text(stringResource(Res.string.locations_rename)) },
				leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
				onClick = {
					menuExpanded = false
					onRename()
				},
			)
			DropdownMenuItem(
				text = { Text(stringResource(Res.string.common_delete)) },
				leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
				onClick = {
					menuExpanded = false
					onDelete()
				},
			)
		}
	}
}

@Composable
private fun LocationCardContent(
	item: PressedLocationWithWatch,
	onClick: () -> Unit,
	onLongClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val location = item.location
	val pressedAt = LocalLocaleFormatter.current.formatDateTime(location.pressedAt)
	CardView(
		modifier = modifier,
		onClick = onClick,
		onLongClick = onLongClick,
		title = { Text(text = location.name ?: pressedAt, style = MaterialTheme.typography.titleMedium) },
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			Text(text = item.watchName, style = MaterialTheme.typography.bodyMedium)
			// an unnamed location already has the time as its title
			if (location.name != null) {
				Text(text = pressedAt, style = MaterialTheme.typography.bodyMedium)
			}
			Text(
				text = stringResource(
					Res.string.locations_coordinates,
					formatCoordinate(location.latitude),
					formatCoordinate(location.longitude),
				),
				style = MaterialTheme.typography.bodyMedium,
			)
			location.accuracyMeters?.let {
				Text(
					text = stringResource(Res.string.locations_accuracy, it.roundToInt()),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun LocationCardPreview() {
	SkuareTheme {
		LocationCard(
			item = PressedLocationWithWatch(
				location = PressedLocation(
					id = 1,
					watchAddress = "D8:70:90:50:94:26",
					watchName = "CASIO GW-B5600#",
					pressedAt = Instant.fromEpochMilliseconds(1792231646000),
					latitude = 22.2793,
					longitude = 114.16281,
					accuracyMeters = 12.4,
					name = "Secret hideout",
				),
				pairedDisplayName = "My watch",
			),
			onClick = {},
			onRename = {},
			onDelete = {},
		)
	}
}
