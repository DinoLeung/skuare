package xyz.d1n0.ui.screen.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.d1n0.Log
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.phone.LocationProvider
import xyz.d1n0.sync.WatchPresenceMonitor
import kotlin.coroutines.cancellation.CancellationException

/**
 * @property isLocationSupported Whether location recording can work here. It runs in background
 *           sessions, so it needs background sync too.
 * @property isLocationPermissionGranted Whether a background session may read the location now.
 * @property locationPermissionDenied Turning location recording on failed, the user didn't allow it.
 * @property isRequestingPermission A permission dialog is open, the switch waits for it.
 */
data class AppSettingsUiState(
	val isLocationSupported: Boolean = false,
	val isLocationRecordingEnabled: Boolean = false,
	val isLocationPermissionGranted: Boolean = false,
	val locationPermissionDenied: Boolean = false,
	val isRequestingPermission: Boolean = false,
) {
	/** On, but the permission was taken away in the system settings since. */
	val isLocationPermissionMissing: Boolean
		get() = isLocationRecordingEnabled && !isLocationPermissionGranted
}

/**
 * @param permissions Requests through the screen's moko controller, see [asRequester].
 */
class AppSettingsScreenViewModel(
	private val permissions: PermissionRequester,
	private val appSettingsRepository: AppSettingsRepository,
	private val locationProvider: LocationProvider,
	presenceMonitor: WatchPresenceMonitor,
) : ViewModel() {
	private val log = Log

	private val _state = MutableStateFlow(
		AppSettingsUiState(isLocationSupported = locationProvider.isSupported && presenceMonitor.isSupported)
	)
	val state: StateFlow<AppSettingsUiState> = _state.asStateFlow()

	init {
		viewModelScope.launch {
			appSettingsRepository.observeLocationRecordingEnabled().collect { enabled ->
				_state.update { it.copy(isLocationRecordingEnabled = enabled) }
			}
		}
		refreshPermissions()
	}

	/** Reads the permission again, e.g. on resume, the user may have changed it in the system settings. */
	fun refreshPermissions() {
		_state.update { it.copy(isLocationPermissionGranted = locationProvider.isPermissionGranted()) }
	}

	fun openSystemSettings() = permissions.openAppSettings()

	/**
	 * Turning it on asks for the permissions it needs first, and stays off if any is refused.
	 * Turning it off asks for nothing, the system settings take a permission away.
	 */
	fun setLocationRecording(enabled: Boolean) {
		if (_state.value.isRequestingPermission) return
		if (!enabled) {
			_state.update { it.copy(locationPermissionDenied = false) }
			viewModelScope.launch { appSettingsRepository.setLocationRecordingEnabled(false) }
			return
		}
		viewModelScope.launch {
			_state.update { it.copy(isRequestingPermission = true, locationPermissionDenied = false) }
			try {
				val granted = requestLocationPermissions()
				if (granted) appSettingsRepository.setLocationRecordingEnabled(true)
				_state.update { it.copy(locationPermissionDenied = !granted) }
			} finally {
				_state.update { it.copy(isRequestingPermission = false) }
				refreshPermissions()
			}
		}
	}

	/** Asks for each of [LocationProvider.requiredPermissions] in turn, stops at the first refused. */
	private suspend fun requestLocationPermissions(): Boolean {
		for (permission in locationProvider.requiredPermissions) {
			if (permissions.isGranted(permission)) continue
			try {
				permissions.request(permission)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				log.i { "Location permission not granted: ${e.message}" }
				return false
			}
		}
		return locationProvider.isPermissionGranted()
	}
}
