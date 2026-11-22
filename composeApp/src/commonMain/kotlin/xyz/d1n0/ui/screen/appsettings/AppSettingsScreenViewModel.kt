package xyz.d1n0.ui.screen.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.d1n0.Log
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.data.repository.FindPhoneSettings
import xyz.d1n0.phone.FindPhoneSounds
import xyz.d1n0.phone.LocationProvider
import xyz.d1n0.phone.PhoneFinder
import xyz.d1n0.phone.PickedAudio
import xyz.d1n0.sync.WatchPresenceMonitor
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @property isLocationSupported Whether location recording can work here. It runs in background
 *           sessions, so it needs background sync too.
 * @property isLocationPermissionGranted Whether a background session may read the location now.
 * @property locationPermissionDenied Turning location recording on failed, the user didn't allow it.
 * @property isFindPhoneSupported Whether find phone can work here, background sessions again.
 * @property canForceMaxVolume False on iOS, the sound follows the phone's volume there.
 * @property isNotificationPermissionGranted The Stop button of the ringing lives in a notification.
 * @property notificationPermissionDenied Turning find phone on failed, notifications weren't allowed.
 * @property isRequestingPermission A permission prompt is open, the switches wait for it.
 * @property isCustomSoundMissing A ringtone is set but its copy is gone, the built-in tone rings.
 * @property soundRejection Why the last picked file wasn't taken, until the next pick or reset.
 * @property isTakingSound A picked file is being read and checked.
 * @property isPlayingSound The sound plays until stopped, see [AppSettingsScreenViewModel.toggleSoundPlayback].
 */
data class AppSettingsUiState(
	val isLocationSupported: Boolean = false,
	val isLocationRecordingEnabled: Boolean = false,
	val isLocationPermissionGranted: Boolean = false,
	val locationPermissionDenied: Boolean = false,
	val isFindPhoneSupported: Boolean = false,
	val canForceMaxVolume: Boolean = true,
	val findPhone: FindPhoneSettings = FindPhoneSettings(),
	val isNotificationPermissionGranted: Boolean = false,
	val notificationPermissionDenied: Boolean = false,
	val isRequestingPermission: Boolean = false,
	val isCustomSoundMissing: Boolean = false,
	val soundRejection: FindPhoneSounds.Rejection? = null,
	val isTakingSound: Boolean = false,
	val isPlayingSound: Boolean = false,
) {
	/** On, but the permission was taken away in the system settings since. */
	val isLocationPermissionMissing: Boolean
		get() = isLocationRecordingEnabled && !isLocationPermissionGranted

	/** On, but notifications were turned off in the system settings since, it rings without Stop. */
	val isNotificationPermissionMissing: Boolean
		get() = findPhone.enabled && !isNotificationPermissionGranted
}

/**
 * @param permissions Requests through the screen's moko controller, see [asRequester].
 */
class AppSettingsScreenViewModel(
	private val permissions: PermissionRequester,
	private val appSettingsRepository: AppSettingsRepository,
	private val locationProvider: LocationProvider,
	phoneFinder: PhoneFinder,
	private val sounds: FindPhoneSounds,
	presenceMonitor: WatchPresenceMonitor,
) : ViewModel() {
	private val log = Log

	private var playback: Job? = null

	private val _state = MutableStateFlow(
		AppSettingsUiState(
			isLocationSupported = locationProvider.isSupported && presenceMonitor.isSupported,
			isFindPhoneSupported = phoneFinder.isSupported && presenceMonitor.isSupported,
			canForceMaxVolume = phoneFinder.canForceMaxVolume,
		)
	)
	val state: StateFlow<AppSettingsUiState> = _state.asStateFlow()

	init {
		viewModelScope.launch {
			appSettingsRepository.observeLocationRecordingEnabled().collect { enabled ->
				_state.update { it.copy(isLocationRecordingEnabled = enabled) }
			}
		}
		viewModelScope.launch {
			appSettingsRepository.observeFindPhoneSettings().collect { settings ->
				_state.update { it.copy(findPhone = settings) }
				checkCustomSound()
			}
		}
		refreshPermissions()
	}

	/**
	 * Reads the permissions again, e.g. on resume, the user may have changed them in the system
	 * settings. Checks the ringtone's copy too.
	 */
	fun refreshPermissions() {
		_state.update { it.copy(isLocationPermissionGranted = locationProvider.isPermissionGranted()) }
		viewModelScope.launch {
			val granted = permissions.isGranted(Permission.REMOTE_NOTIFICATION)
			_state.update { it.copy(isNotificationPermissionGranted = granted) }
			checkCustomSound()
		}
	}

	/** Takes [picked] as the ringtone, or says why not and keeps the previous one, see [FindPhoneSounds.choose]. */
	fun chooseSound(picked: PickedAudio) {
		if (_state.value.isTakingSound) return
		viewModelScope.launch {
			_state.update { it.copy(isTakingSound = true, soundRejection = null) }
			try {
				val rejection = sounds.choose(picked)
				_state.update { it.copy(soundRejection = rejection) }
			} finally {
				_state.update { it.copy(isTakingSound = false) }
				checkCustomSound()
			}
		}
	}

	/** Back to the built-in tone. */
	fun resetSound() {
		viewModelScope.launch {
			_state.update { it.copy(soundRejection = null) }
			sounds.reset()
			checkCustomSound()
		}
	}

	/**
	 * Plays the sound at the phone's current volume until called again, or stops it if it plays. A
	 * whole track can be heard this way, not only its start. See [stopSoundPlayback] for leaving.
	 */
	fun toggleSoundPlayback() {
		if (playback?.isActive == true) {
			stopSoundPlayback()
			return
		}
		playback = viewModelScope.launch {
			_state.update { it.copy(isPlayingSound = true) }
			try {
				sounds.playAtCurrentVolume()
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				log.w { "Playing the sound failed: ${e.message}" }
			} finally {
				_state.update { it.copy(isPlayingSound = false) }
			}
		}
	}

	/**
	 * Stops Play sound, also when the screen goes to the background, so it doesn't keep playing
	 * where the user can't see Stop. Leaving the screen stops it too, with the view model.
	 */
	fun stopSoundPlayback() {
		playback?.cancel()
	}

	private suspend fun checkCustomSound() {
		val missing = sounds.isCustomSoundMissing()
		_state.update { it.copy(isCustomSoundMissing = missing) }
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
		requestingPermissions {
			val granted = requestAll(locationProvider.requiredPermissions) && locationProvider.isPermissionGranted()
			if (granted) appSettingsRepository.setLocationRecordingEnabled(true)
			_state.update { it.copy(locationPermissionDenied = !granted) }
		}
	}

	/**
	 * Turning it on asks for notifications first, the ringing's Stop button is in one, and stays off
	 * if they're refused. Turning it off asks for nothing.
	 */
	fun setFindPhone(enabled: Boolean) {
		if (_state.value.isRequestingPermission) return
		if (!enabled) {
			_state.update { it.copy(notificationPermissionDenied = false) }
			viewModelScope.launch { appSettingsRepository.setFindPhoneEnabled(false) }
			return
		}
		requestingPermissions {
			val granted = requestAll(listOf(Permission.REMOTE_NOTIFICATION))
			if (granted) appSettingsRepository.setFindPhoneEnabled(true)
			_state.update { it.copy(notificationPermissionDenied = !granted) }
		}
	}

	fun setRingUntilStopped(untilStopped: Boolean) {
		viewModelScope.launch { appSettingsRepository.setFindPhoneRingUntilStopped(untilStopped) }
	}

	/** See [FindPhoneSettings.maxVolume]. */
	fun setMaxVolume(maxVolume: Boolean) {
		viewModelScope.launch { appSettingsRepository.setFindPhoneMaxVolume(maxVolume) }
	}

	/**
	 * Stores [length] if it's at least a second, see [isValidRingLength]. The screen shows anything
	 * shorter as an error and the stored length stays.
	 */
	fun setRingLength(length: Duration) {
		if (!isValidRingLength(length) || length == _state.value.findPhone.ringLength) return
		viewModelScope.launch { appSettingsRepository.setFindPhoneRingLength(length) }
	}

	/** Marks the switches as waiting for [block], which asks for permissions, and reads them after. */
	private fun requestingPermissions(block: suspend () -> Unit) {
		viewModelScope.launch {
			_state.update { it.copy(isRequestingPermission = true) }
			try {
				block()
			} finally {
				_state.update { it.copy(isRequestingPermission = false) }
				refreshPermissions()
			}
		}
	}

	/** Asks for each of [required] that isn't granted, in turn, stops at the first refused. */
	private suspend fun requestAll(required: List<Permission>): Boolean {
		for (permission in required) {
			if (permissions.isGranted(permission)) continue
			try {
				permissions.request(permission)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				log.i { "Permission not granted: ${e.message}" }
				return false
			}
		}
		return true
	}

	companion object {
		/** A ring length the user can store, at least a second. */
		fun isValidRingLength(length: Duration) = length >= 1.seconds
	}
}
