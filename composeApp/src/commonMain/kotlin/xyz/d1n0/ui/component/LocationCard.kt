package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * A recorded location: the watch, when its lower-right button was pressed, and where the phone was.
 */
@Composable
fun LocationCard(
	item: PressedLocationWithWatch,
	modifier: Modifier = Modifier,
) {
	val location = item.location
	CardView(
		modifier = modifier,
		title = { Text(text = item.watchName, style = MaterialTheme.typography.titleMedium) },
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp),
		) {
			Text(
				text = LocalLocaleFormatter.current.formatDateTime(location.pressedAt),
				style = MaterialTheme.typography.bodyMedium,
			)
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
				),
				pairedDisplayName = "My watch",
			),
		)
	}
}
