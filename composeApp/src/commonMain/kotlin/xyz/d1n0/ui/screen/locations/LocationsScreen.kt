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
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.Log
import xyz.d1n0.data.database.PressedLocation
import xyz.d1n0.data.database.PressedLocationWithWatch
import xyz.d1n0.phone.GeoPoint
import xyz.d1n0.phone.MapLinkBuilder
import xyz.d1n0.resources.*
import xyz.d1n0.ui.component.LocationCard
import xyz.d1n0.ui.text.LocalLocaleFormatter
import xyz.d1n0.ui.text.formatDateTime
import xyz.d1n0.ui.theme.SkuareTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

/**
 * Where the phone was at each short press of the lower-right button, newest first. Tapping one
 * opens it in the phone's map app, a long press offers to rename or delete it. The title bar deletes
 * the older ones or all of them.
 */
@Composable
fun LocationsScreen(onBack: () -> Unit) {
	val viewModel = koinViewModel<LocationsScreenViewModel>()
	val state by viewModel.state.collectAsState()
	val mapLinks = koinInject<MapLinkBuilder>()
	val uriHandler = LocalUriHandler.current
	val formatter = LocalLocaleFormatter.current
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()

	LocationsScreenContent(
		state = state,
		snackbarHostState = snackbarHostState,
		onBack = onBack,
		onOpenInMaps = { location ->
			val point = GeoPoint(location.latitude, location.longitude, location.accuracyMeters)
			// the pin is named after the user's name for the place, else the press, the map app shows the
			// place itself
			val label = location.name ?: formatter.formatDateTime(location.pressedAt)
			val link = mapLinks.link(point, label = label)
			try {
				uriHandler.openUri(link)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				// e.g. Android without an app for geo: links. Neither the link nor the message, which
				// repeats it, is logged: they hold the location
				Log.w { "Can't open the location in a map app: ${e::class.simpleName}" }
				scope.launch { snackbarHostState.showSnackbar(getString(Res.string.locations_no_map_app)) }
			}
		},
		onDelete = { location ->
			viewModel.delete(location)
			scope.launch {
				// a later delete replaces the snackbar, only the last one can be undone
				snackbarHostState.currentSnackbarData?.dismiss()
				val result = snackbarHostState.showSnackbar(
					message = getString(Res.string.locations_deleted),
					actionLabel = getString(Res.string.locations_undo),
					duration = SnackbarDuration.Short,
				)
				if (result == SnackbarResult.ActionPerformed) viewModel.restore(location)
			}
		},
		onRename = viewModel::rename,
		olderThan = viewModel::olderThan,
		// a bulk delete supersedes a single one, its Undo goes, see LocationsScreenViewModel.restore
		onDeleteBefore = { cutoff ->
			snackbarHostState.currentSnackbarData?.dismiss()
			viewModel.deleteBefore(cutoff)
		},
		onDeleteAll = {
			snackbarHostState.currentSnackbarData?.dismiss()
			viewModel.deleteAll()
		},
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationsScreenContent(
	state: LocationsUiState,
	snackbarHostState: SnackbarHostState,
	onBack: () -> Unit,
	onOpenInMaps: (PressedLocation) -> Unit,
	onDelete: (PressedLocation) -> Unit,
	onRename: (PressedLocation, String) -> Unit,
	olderThan: () -> Map<LocationAge, OlderThan>,
	onDeleteBefore: (Instant) -> Unit,
	onDeleteAll: () -> Unit,
) {
	val locations = state.locations
	// taken when the dialog opens: the counts don't change under the user's eyes, and confirming
	// deletes with the cutoff they were counted with
	var olderThanChoices by remember { mutableStateOf<Map<LocationAge, OlderThan>?>(null) }
	var showDeleteAllDialog by remember { mutableStateOf(false) }
	var renaming by remember { mutableStateOf<PressedLocation?>(null) }

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
				actions = {
					if (!locations.isNullOrEmpty()) {
						IconButton(onClick = { olderThanChoices = olderThan() }) {
							Icon(
								Icons.Outlined.AutoDelete,
								contentDescription = stringResource(Res.string.locations_delete_older),
							)
						}
						IconButton(onClick = { showDeleteAllDialog = true }) {
							Icon(
								Icons.Outlined.DeleteSweep,
								contentDescription = stringResource(Res.string.locations_delete_all),
							)
						}
					}
				},
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
	) { scaffoldPadding ->
		if (locations == null) return@Scaffold
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
				items(locations, key = { it.location.id }) { item ->
					LocationCard(
						item = item,
						onClick = { onOpenInMaps(item.location) },
						onRename = { renaming = item.location },
						onDelete = { onDelete(item.location) },
						modifier = Modifier.fillMaxWidth(),
					)
				}
			}
		}
	}

	renaming?.let { location ->
		RenameLocationDialog(
			name = location.name,
			defaultName = LocalLocaleFormatter.current.formatDateTime(location.pressedAt),
			onConfirm = { name ->
				renaming = null
				onRename(location, name)
			},
			onDismiss = { renaming = null },
		)
	}

	olderThanChoices?.let { choices ->
		DeleteOlderLocationsDialog(
			counts = choices.mapValues { it.value.count },
			onConfirm = { age ->
				olderThanChoices = null
				onDeleteBefore(choices.getValue(age).cutoff)
			},
			onDismiss = { olderThanChoices = null },
		)
	}

	if (showDeleteAllDialog) {
		DeleteAllLocationsDialog(
			count = locations?.size ?: 0,
			onConfirm = {
				showDeleteAllDialog = false
				onDeleteAll()
			},
			onDismiss = { showDeleteAllDialog = false },
		)
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
							name = "Secret hideout",
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
			snackbarHostState = remember { SnackbarHostState() },
			onBack = {},
			onOpenInMaps = {},
			onDelete = {},
			onRename = { _, _ -> },
			olderThan = { emptyMap() },
			onDeleteBefore = {},
			onDeleteAll = {},
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun LocationsScreenEmptyPreview() {
	SkuareTheme {
		LocationsScreenContent(
			state = LocationsUiState(locations = emptyList()),
			snackbarHostState = remember { SnackbarHostState() },
			onBack = {},
			onOpenInMaps = {},
			onDelete = {},
			onRename = { _, _ -> },
			olderThan = { emptyMap() },
			onDeleteBefore = {},
			onDeleteAll = {},
		)
	}
}
