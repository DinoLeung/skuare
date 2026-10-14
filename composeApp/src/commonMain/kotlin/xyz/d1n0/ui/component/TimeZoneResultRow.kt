package xyz.d1n0.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.model.HomeTimezone
import xyz.d1n0.lib.model.Timezone
import xyz.d1n0.lib.model.WorldTimezone

@Composable
internal fun TimeZoneResultRow(
	timeZone: Timezone,
	selected: Boolean,
	onClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.heightIn(min = 64.dp)
			.clickable(onClick = onClick)
			.padding(horizontal = 12.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		RadioButton(
			selected = selected,
			onClick = onClick,
		)
		Column(
			modifier = Modifier
				.weight(1f)
				.padding(horizontal = 8.dp),
		) {
			Text(
				text = timeZone.primaryLabel,
				style = MaterialTheme.typography.bodyLarge,
			)
			timeZone.secondaryLabel?.let { label ->
				Text(
					text = label,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
		Text(
			text = timeZone.offsetString(),
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
	}
}

private val Timezone.primaryLabel: String
	get() = when (this) {
		is WorldTimezone -> city
		else -> cityName.lowercase().replaceFirstChar { it.uppercase() }
	}

private val Timezone.secondaryLabel: String?
	get() = when (this) {
		is WorldTimezone -> country.ifBlank { null }
		is HomeTimezone -> timeZone
		else -> null
	}
