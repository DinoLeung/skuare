package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import xyz.d1n0.lib.constant.WorldTimezoneData
import xyz.d1n0.lib.helper.searchTimeZones
import xyz.d1n0.lib.model.HomeTimezone
import xyz.d1n0.lib.model.Timezone
import xyz.d1n0.lib.model.WorldTimezone

/**
 * Selects a timezone from either [HomeTimezone] or [WorldTimezone] data.
 *
 * Selection is kept as dialog-local draft state and is only emitted through [onConfirm].
 * [timeZones] should contain one timezone family because the returned value has the same
 * generic type as the supplied collection and current [timeZone].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Timezone> TimeZonePickerDialog(
	timeZone: T,
	timeZones: Collection<T>,
	visible: Boolean,
	onConfirm: (T) -> Unit,
	onDismiss: () -> Unit,
	title: String = "Select time zone",
) {
	if (!visible) return

	var query by remember { mutableStateOf("") }
	var selectedTimeZone by remember(timeZone.identifier) { mutableStateOf(timeZone) }
	val results = remember(query, timeZones, timeZone.identifier) {
		searchTimeZones(timeZones, query).let { matches ->
			if (query.isBlank()) matches.withTimeZoneFirst(timeZone.identifier) else matches
		}
	}
	val listState = rememberLazyListState()

	LaunchedEffect(query, timeZone.identifier) {
		if (results.isNotEmpty()) listState.scrollToItem(0)
	}

	BasicAlertDialog(
		onDismissRequest = onDismiss,
		modifier = Modifier.wrapContentSize(),
		properties = DialogProperties(usePlatformDefaultWidth = false),
	) {
		Surface(
			modifier = Modifier.wrapContentSize(),
			shape = MaterialTheme.shapes.extraLarge,
			tonalElevation = AlertDialogDefaults.TonalElevation,
		) {
			Column {
				Text(
					text = title,
					style = MaterialTheme.typography.headlineSmall,
					modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp),
				)
				OutlinedTextField(
					value = query,
					onValueChange = { query = it },
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 24.dp, vertical = 16.dp),
					singleLine = true,
					label = { Text("Time zone") },
					leadingIcon = {
						Icon(
							imageVector = Icons.Outlined.Search,
							contentDescription = null,
						)
					},
					trailingIcon = if (query.isNotEmpty()) {
						{
							IconButton(onClick = { query = "" }) {
								Icon(
									imageVector = Icons.Outlined.Clear,
									contentDescription = "Clear search",
								)
							}
						}
					} else null,
				)
				HorizontalDivider()
				if (results.isEmpty()) {
					Column(
						modifier = Modifier
							.weight(1f)
							.fillMaxWidth()
							.padding(24.dp),
						verticalArrangement = Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Text(
							text = "No matching time zones",
							style = MaterialTheme.typography.bodyLarge,
						)
					}
				} else {
					LazyColumn(
						state = listState,
						modifier = Modifier
							.weight(1f)
							.fillMaxWidth()
							.clipToBounds(),
					) {
						items(
							items = results,
							key = { it.identifier },
						) { result ->
							TimeZoneResultRow(
								timeZone = result,
								selected = result.identifier == selectedTimeZone.identifier,
								onClick = { selectedTimeZone = result },
							)
						}
					}
				}
				Surface(
					color = MaterialTheme.colorScheme.surface,
				) {
					Column {
						HorizontalDivider()
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 16.dp, vertical = 8.dp),
							horizontalArrangement = Arrangement.End,
						) {
							TextButton(onClick = onDismiss) { Text("Cancel") }
							TextButton(onClick = { onConfirm(selectedTimeZone) }) { Text("Confirm") }
						}
					}
				}
			}
		}
	}
}

internal fun <T : Timezone> List<T>.withTimeZoneFirst(identifier: Short): List<T> {
	val selectedIndex = indexOfFirst { it.identifier == identifier }
	if (selectedIndex <= 0) return this

	return buildList(size) {
		add(this@withTimeZoneFirst[selectedIndex])
		this@withTimeZoneFirst.forEachIndexed { index, timeZone ->
			if (index != selectedIndex) add(timeZone)
		}
	}
}

@Preview(showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun TimeZonePickerDialogPreview() {
	val timeZones = WorldTimezoneData.values.toList()
	val selectedTimeZone = timeZones.find { it.city == "Hong Kong" } ?: timeZones.first()
	MaterialTheme {
		TimeZonePickerDialog(
			timeZone = selectedTimeZone,
			timeZones = timeZones,
			visible = true,
			onConfirm = {},
			onDismiss = {},
		)
	}
}
