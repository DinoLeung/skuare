package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.Clock
import xyz.d1n0.lib.model.DstSettings
import xyz.d1n0.lib.model.HomeClock

@Composable
fun ClockCard(
	clock: Clock,
	modifier: Modifier = Modifier,
) {
	CardView(modifier = modifier, leadingIcon = {
		if (clock is HomeClock) {
			Icon(
				imageVector = Icons.Sharp.Home,
				contentDescription = "Home Time",
			)
		} else {
			Icon(
				imageVector = Icons.Sharp.Public,
				contentDescription = "World Time",
			)
		}
	}, title = {
		Text(
			text = clock.timeZone.cityName.lowercase().replaceFirstChar { it.uppercase() },
			style = MaterialTheme.typography.headlineSmall,
		)
	}, indicator = {
		AssistChip(
			enabled = clock.dstSettings.enable,
			label = { Text("DST") },
			onClick = { },
		)
	}, content = {
		Text(
			text = clock.offsetString(),
			textAlign = TextAlign.End,
			modifier = Modifier.fillMaxWidth(),
			style = MaterialTheme.typography.titleMedium,
		)
	})
}

@Preview
@Composable
private fun ClockCardPreview() {
	Column {
		ClockCard(
			clock = xyz.d1n0.lib.model.HomeClock.fromTimeZoneId(30335, DstSettings(false, true))
		)
		ClockCard(
			clock = xyz.d1n0.lib.model.WorldClock.fromTimeZoneId(95, DstSettings(true, false))
		)
	}
}
