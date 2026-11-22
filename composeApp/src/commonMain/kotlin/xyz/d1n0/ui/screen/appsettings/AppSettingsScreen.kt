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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.d1n0.data.repository.FindPhoneSettings
import xyz.d1n0.resources.*
import xyz.d1n0.ui.component.CardView
import xyz.d1n0.ui.component.DurationTextInput
import xyz.d1n0.ui.component.ErrorText
import xyz.d1n0.ui.component.SwitchField
import xyz.d1n0.ui.theme.SkuareTheme
import kotlin.time.Duration

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
		onFindPhoneChange = viewModel::setFindPhone,
		onRingUntilStoppedChange = viewModel::setRingUntilStopped,
		onRingLengthChange = viewModel::setRingLength,
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
	onFindPhoneChange: (Boolean) -> Unit,
	onRingUntilStoppedChange: (Boolean) -> Unit,
	onRingLengthChange: (Duration) -> Unit,
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
			item {
				FindPhoneCard(
					state = state,
					onFindPhoneChange = onFindPhoneChange,
					onRingUntilStoppedChange = onRingUntilStoppedChange,
					onRingLengthChange = onRingLengthChange,
					onOpenSystemSettings = onOpenSystemSettings,
					modifier = Modifier.fillMaxWidth(),
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
	SettingCard(title = Res.string.app_settings_location_title, modifier = modifier) {
		if (!state.isLocationSupported) {
			Description(Res.string.app_settings_location_unsupported)
			return@SettingCard
		}
		SwitchField(
			modifier = Modifier.fillMaxWidth(),
			title = stringResource(Res.string.app_settings_location_switch),
			check = state.isLocationRecordingEnabled,
			onCheckedChange = onLocationRecordingChange,
			enabled = !state.isRequestingPermission,
		)
		Description(Res.string.app_settings_location_description)
		PermissionProblem(
			problem = when {
				state.isLocationPermissionMissing -> Res.string.app_settings_location_permission_missing
				state.locationPermissionDenied -> Res.string.app_settings_location_permission_denied
				else -> null
			},
			onOpenSystemSettings = onOpenSystemSettings,
		)
	}
}

@Composable
private fun FindPhoneCard(
	state: AppSettingsUiState,
	onFindPhoneChange: (Boolean) -> Unit,
	onRingUntilStoppedChange: (Boolean) -> Unit,
	onRingLengthChange: (Duration) -> Unit,
	onOpenSystemSettings: () -> Unit,
	modifier: Modifier = Modifier,
) {
	SettingCard(title = Res.string.app_settings_find_phone_title, modifier = modifier) {
		if (!state.isFindPhoneSupported) {
			Description(Res.string.app_settings_find_phone_unsupported)
			return@SettingCard
		}
		val findPhone = state.findPhone
		SwitchField(
			modifier = Modifier.fillMaxWidth(),
			title = stringResource(Res.string.app_settings_find_phone_switch),
			check = findPhone.enabled,
			onCheckedChange = onFindPhoneChange,
			enabled = !state.isRequestingPermission,
		)
		Description(Res.string.app_settings_find_phone_description)
		if (!state.canForceMaxVolume) Description(Res.string.app_settings_find_phone_volume_note)
		PermissionProblem(
			problem = when {
				state.isNotificationPermissionMissing -> Res.string.app_settings_find_phone_notifications_missing
				state.notificationPermissionDenied -> Res.string.app_settings_find_phone_notifications_denied
				else -> null
			},
			onOpenSystemSettings = onOpenSystemSettings,
		)
		SwitchField(
			modifier = Modifier.fillMaxWidth(),
			title = stringResource(Res.string.app_settings_find_phone_until_stopped),
			check = findPhone.ringUntilStopped,
			onCheckedChange = onRingUntilStoppedChange,
		)
		RingLengthField(
			stored = findPhone.ringLength,
			enabled = !findPhone.ringUntilStopped,
			onRingLengthChange = onRingLengthChange,
		)
	}
}

/**
 * The field keeps what the user types, only a valid length is stored. While it's too short, it
 * shows an error and the stored length stays.
 */
@Composable
private fun RingLengthField(
	stored: Duration,
	enabled: Boolean,
	onRingLengthChange: (Duration) -> Unit,
) {
	var typed by remember { mutableStateOf(stored) }
	// e.g. the stored length loading after the screen opened
	LaunchedEffect(stored) { typed = stored }
	val isValid = AppSettingsScreenViewModel.isValidRingLength(typed)
	val error: (@Composable () -> Unit)? =
		if (isValid) null
		else { { Text(stringResource(Res.string.app_settings_find_phone_ring_length_error)) } }
	DurationTextInput(
		duration = typed,
		onDurationChange = {
			typed = it
			onRingLengthChange(it)
		},
		label = { Text(stringResource(Res.string.app_settings_find_phone_ring_for)) },
		supportingText = error,
		isError = enabled && !isValid,
		enabled = enabled,
		modifier = Modifier.fillMaxWidth(),
	)
}

@Composable
private fun SettingCard(
	title: StringResource,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit,
) {
	CardView(
		modifier = modifier,
		title = { Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium) },
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			content()
		}
	}
}

@Composable
private fun Description(text: StringResource) {
	Text(
		text = stringResource(text),
		style = MaterialTheme.typography.bodySmall,
		color = MaterialTheme.colorScheme.onSurfaceVariant,
	)
}

/** What's wrong with a permission, and the button to fix it in the system settings. */
@Composable
private fun PermissionProblem(problem: StringResource?, onOpenSystemSettings: () -> Unit) {
	problem ?: return
	ErrorText(stringResource(problem))
	TextButton(onClick = onOpenSystemSettings) {
		Text(stringResource(Res.string.app_settings_open_system_settings))
	}
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun AppSettingsScreenPreview() {
	SkuareTheme {
		AppSettingsScreenContent(
			state = AppSettingsUiState(
				isLocationSupported = true,
				isLocationRecordingEnabled = true,
				isLocationPermissionGranted = false,
				isFindPhoneSupported = true,
				canForceMaxVolume = false,
				findPhone = FindPhoneSettings(enabled = true),
				isNotificationPermissionGranted = true,
			),
			onBack = {},
			onLocationRecordingChange = {},
			onFindPhoneChange = {},
			onRingUntilStoppedChange = {},
			onRingLengthChange = {},
			onOpenSystemSettings = {},
			onOpenLocations = {},
		)
	}
}
