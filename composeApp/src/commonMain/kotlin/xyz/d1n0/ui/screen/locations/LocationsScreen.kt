package xyz.d1n0.ui.screen.locations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.data.database.PressedLocation
import xyz.d1n0.data.database.PressedLocationWithWatch
import xyz.d1n0.resources.*
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.text.LocalLocaleFormatter
import xyz.d1n0.ui.text.formatCoordinate
import xyz.d1n0.ui.text.formatDateTime
import xyz.d1n0.ui.theme.SkuareTheme
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * Where the phone was at each short press of the lower-right button, newest first. Read only for
 * now, see research/button-actions-plan.md.
 */
@Composable
fun LocationsScreen(onBack: () -> Unit) {
	val viewModel = koinViewModel<LocationsScreenViewModel>()
	val state by viewModel.state.collectAsState()
	LocationsScreenContent(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationsScreenContent(
	state: LocationsUiState,
	onBack: () -> Unit,
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(Res.string.locations_title)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(Res.string.app_settings_back),
						)
					}
				},
			)
		},
	) { scaffoldPadding ->
		val locations = state.locations ?: return@Scaffold
		if (locations.isEmpty()) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(scaffoldPadding)
					.padding(32.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
			) {
				Text(text = stringResource(Res.string.locations_empty), style = MaterialTheme.typography.titleMedium)
				Text(
					text = stringResource(
						if (state.isLocationRecordingEnabled) Res.string.locations_empty_hint_on
						else Res.string.locations_empty_hint_off
					),
					style = MaterialTheme.typography.bodyMedium,
					textAlign = TextAlign.Center,
				)
			}
		} else {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(scaffoldPadding),
				contentPadding = PaddingValues(8.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
			) {
				items(locations, key = { it.location.id }) { location ->
					LocationCard(location, modifier = Modifier.fillMaxWidth())
				}
			}
		}
	}
}

@Composable
private fun LocationCard(item: PressedLocationWithWatch, modifier: Modifier = Modifier) {
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
private fun LocationsScreenPreview() {
	SkuareTheme {
		LocationsScreenContent(
			state = LocationsUiState(
				locations = listOf(
					PressedLocationWithWatch(
						location = PressedLocation(
							id = 2,
							watchAddress = "D8:70:90:50:94:26",
							watchName = "CASIO GW-B5600#",
							pressedAt = Instant.fromEpochMilliseconds(1792231646000),
							latitude = 22.2793,
							longitude = 114.16281,
							accuracyMeters = 12.4,
						),
						pairedDisplayName = "My watch",
					),
					PressedLocationWithWatch(
						location = PressedLocation(
							id = 1,
							watchAddress = "AA:BB:CC:DD:EE:FF",
							watchName = "CASIO GA-B2100",
							pressedAt = Instant.fromEpochMilliseconds(1792220000000),
							latitude = -33.86882,
							longitude = 151.20929,
							accuracyMeters = null,
						),
						pairedDisplayName = null,
					),
				),
			),
			onBack = {},
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun LocationsScreenEmptyPreview() {
	SkuareTheme {
		LocationsScreenContent(state = LocationsUiState(locations = emptyList()), onBack = {})
	}
}
