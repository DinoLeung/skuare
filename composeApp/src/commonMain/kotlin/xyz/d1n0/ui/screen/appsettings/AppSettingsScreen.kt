package xyz.d1n0.ui.screen.appsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.d1n0.resources.*
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.component.ErrorText
import xyz.d1n0.ui.component.SwitchField
import xyz.d1n0.ui.theme.SkuareTheme

/**
 * Settings of the app itself, not of a watch, so it opens from the pairing screen too. Changes are
 * saved straight away, nothing is written to a watch.
 */
@Composable
fun AppSettingsScreen(
	onBack: () -> Unit,
	onOpenLocations: () -> Unit,
) {
	val permissionsControllerFactory = rememberPermissionsControllerFactory()
	val permissionsController =
		remember(permissionsControllerFactory) { permissionsControllerFactory.createPermissionsController() }
	val viewModel = koinViewModel<AppSettingsScreenViewModel>(
		parameters = { parametersOf(permissionsController.asRequester()) }
	)
	val state by viewModel.state.collectAsState()

	BindEffect(permissionsController)
	LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

	AppSettingsScreenContent(
		state = state,
		onBack = onBack,
		onLocationRecordingChange = viewModel::setLocationRecording,
		onOpenSystemSettings = viewModel::openSystemSettings,
		onOpenLocations = onOpenLocations,
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsScreenContent(
	state: AppSettingsUiState,
	onBack: () -> Unit,
	onLocationRecordingChange: (Boolean) -> Unit,
	onOpenSystemSettings: () -> Unit,
	onOpenLocations: () -> Unit,
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(Res.string.app_settings_title)) },
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
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(scaffoldPadding),
			contentPadding = PaddingValues(8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			item {
				LocationRecordingCard(
					state = state,
					onLocationRecordingChange = onLocationRecordingChange,
					onOpenSystemSettings = onOpenSystemSettings,
					modifier = Modifier.fillMaxWidth(),
				)
			}
			item {
				CardView(
					modifier = Modifier.fillMaxWidth(),
					onClick = onOpenLocations,
					title = { Text(stringResource(Res.string.locations_title)) },
					indicator = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
				)
			}
		}
	}
}

@Composable
private fun LocationRecordingCard(
	state: AppSettingsUiState,
	onLocationRecordingChange: (Boolean) -> Unit,
	onOpenSystemSettings: () -> Unit,
	modifier: Modifier = Modifier,
) {
	CardView(
		modifier = modifier,
		title = {
			Text(
				text = stringResource(Res.string.app_settings_location_title),
				style = MaterialTheme.typography.titleMedium,
			)
		},
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			if (!state.isLocationSupported) {
				Text(
					text = stringResource(Res.string.app_settings_location_unsupported),
					style = MaterialTheme.typography.bodyMedium,
				)
				return@Column
			}
			SwitchField(
				modifier = Modifier.fillMaxWidth(),
				title = stringResource(Res.string.app_settings_location_switch),
				check = state.isLocationRecordingEnabled,
				onCheckedChange = onLocationRecordingChange,
				enabled = !state.isRequestingPermission,
			)
			Text(
				text = stringResource(Res.string.app_settings_location_description),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			val problem = when {
				state.isLocationPermissionMissing -> Res.string.app_settings_location_permission_missing
				state.locationPermissionDenied -> Res.string.app_settings_location_permission_denied
				else -> null
			}
			problem?.let {
				ErrorText(stringResource(it))
				TextButton(onClick = onOpenSystemSettings) {
					Text(stringResource(Res.string.app_settings_open_system_settings))
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun AppSettingsScreenPreview() {
	SkuareTheme {
		AppSettingsScreenContent(
			state = AppSettingsUiState(
				isLocationSupported = true,
				isLocationRecordingEnabled = true,
				isLocationPermissionGranted = false,
			),
			onBack = {},
			onLocationRecordingChange = {},
			onOpenSystemSettings = {},
			onOpenLocations = {},
		)
	}
}
