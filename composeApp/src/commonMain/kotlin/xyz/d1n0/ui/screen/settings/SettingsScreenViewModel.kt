package xyz.d1n0.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.constant.AutoSyncDelay
import xyz.d1n0.lib.constant.BacklightDuration
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.DateFormat
import xyz.d1n0.lib.constant.WeekdayLanguage
import xyz.d1n0.lib.model.ConnectionSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.WatchInfo
import xyz.d1n0.lib.model.WatchName
import xyz.d1n0.lib.model.WatchPreferences
import xyz.d1n0.lib.model.WatchSettings
import xyz.d1n0.lib.model.requestConnectionSettings
import xyz.d1n0.lib.model.requestName
import xyz.d1n0.lib.model.requestWatchSettings
import xyz.d1n0.lib.model.writeConnectionSettings
import xyz.d1n0.lib.model.writeName
import xyz.d1n0.lib.model.writeWatchSettings
import xyz.d1n0.ui.boilerplate.updateCatching

data class SettingsUiState(
	val isNameInitialized: Boolean = false,
	val isNameLoading: Boolean = true,
	val savedName: WatchName = defaultWatchName,
	val pendingName: WatchName = defaultWatchName,
	val pendingNameError: Throwable? = null,
	val isWatchSettingsInitialized: Boolean = false,
	val isWatchSettingsLoading: Boolean = true,
	val savedWatchSettings: WatchSettings = defaultWatchSettings,
	val pendingWatchSettings: WatchSettings = defaultWatchSettings,
	val pendingWatchSettingsError: Throwable? = null,
	val isConnectionSettingsInitialized: Boolean = false,
	val isConnectionSettingsLoading: Boolean = true,
	val savedConnectionSettings: ConnectionSettings = defaultConnectionSettings,
	val pendingConnectionSettings: ConnectionSettings = defaultConnectionSettings,
	val pendingConnectionSettingsError: Throwable? = null,
) {
	val isLoading: Boolean
		get() = isNameLoading || isWatchSettingsLoading || isConnectionSettingsLoading

	val hasUpdates: Boolean
		get() = isNameUpdated || isWatchSettingsUpdated || isConnectionSettingsUpdated

	val hasErrors: Boolean
		get() = pendingNameError != null || pendingWatchSettingsError != null || pendingConnectionSettingsError != null

	val isNameUpdated: Boolean
		get() = savedName != pendingName

	val isWatchSettingsUpdated: Boolean
		get() = savedWatchSettings != pendingWatchSettings

	val isConnectionSettingsUpdated: Boolean
		get() = savedConnectionSettings != pendingConnectionSettings
}

sealed interface SettingsUiEvent {
	object RequestName : SettingsUiEvent
	object RequestWatchSettings : SettingsUiEvent
	object RequestConnectionSettings : SettingsUiEvent
	object SaveSettings : SettingsUiEvent
	data class NameInputChange(val name: String) : SettingsUiEvent
	data class Is24HourChange(val is24Hour: Boolean) : SettingsUiEvent
	data class IsMutedChange(val isMuted: Boolean) : SettingsUiEvent
	data class AutoBacklightChange(val enable: Boolean) : SettingsUiEvent
	data class PowerSavingChange(val enable: Boolean) : SettingsUiEvent
	data class BacklightDurationChange(val backlightDuration: BacklightDuration) : SettingsUiEvent
	data class DateFormatChange(val dateFormat: DateFormat) : SettingsUiEvent
	data class WeekdayLanguageChange(val weekdayLanguage: WeekdayLanguage) : SettingsUiEvent
	data class AutoSyncChange(val enable: Boolean) : SettingsUiEvent
	data class AutoSyncDelayChange(val minutes: Int) : SettingsUiEvent
	data class ConnectionTimeoutChange(val connectionTimeout: ConnectionTimeout) : SettingsUiEvent
}

class SettingsScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val watchInfo: StateFlow<WatchInfo> = watch.info

	private val _uiState = MutableStateFlow(SettingsUiState())
	val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			watchInfo.collect { info ->
				_uiState.updateCatching(
					transform = {
						it.copy(
							savedName = info.name ?: defaultWatchName,
							savedWatchSettings = info.watchSettings ?: defaultWatchSettings,
							savedConnectionSettings = info.connectionSettings
								?: defaultConnectionSettings,

							pendingName = info.name ?: it.pendingName,
							pendingWatchSettings = info.watchSettings ?: it.pendingWatchSettings,
							pendingConnectionSettings = info.connectionSettings
								?: it.pendingConnectionSettings
						)
					},
					onSuccess = {
						it.copy(
							isNameInitialized = info.name != null,
							isWatchSettingsInitialized = info.watchSettings != null,
							isConnectionSettingsInitialized = info.connectionSettings != null,
							// Assume updates only comes in on page first load and when fetching updates after writing
							isNameLoading = info.name != it.pendingName,
							isWatchSettingsLoading = info.watchSettings != it.pendingWatchSettings,
							isConnectionSettingsLoading = info.connectionSettings != it.pendingConnectionSettings,
						)
					},
					// It should not be any errors, data is coming from the watch
					onFailure = { copy() },
				)
			}
		}
	}

	fun onEvent(event: SettingsUiEvent) = when (event) {
		SettingsUiEvent.RequestName -> requestName()
		SettingsUiEvent.RequestWatchSettings -> requestWatchSettings()
		SettingsUiEvent.RequestConnectionSettings -> requestConnectionSettings()
		SettingsUiEvent.SaveSettings -> writeSettings()
		is SettingsUiEvent.NameInputChange -> onNameInputChange(name = event.name)
		is SettingsUiEvent.Is24HourChange -> onIs24HourChange(is24Hour = event.is24Hour)
		is SettingsUiEvent.IsMutedChange -> onIsMutedChange(isMuted = event.isMuted)
		is SettingsUiEvent.AutoBacklightChange -> onAutoBacklightChange(enable = event.enable)
		is SettingsUiEvent.PowerSavingChange -> onPowerSavingChange(enable = event.enable)
		is SettingsUiEvent.BacklightDurationChange -> onBacklightDurationChange(backlightDuration = event.backlightDuration)
		is SettingsUiEvent.DateFormatChange -> onDateFormatChange(dateFormat = event.dateFormat)
		is SettingsUiEvent.WeekdayLanguageChange -> onWeekdayLanguageChange(weekdayLanguage = event.weekdayLanguage)
		is SettingsUiEvent.AutoSyncChange -> onAutoSyncChange(enable = event.enable)
		is SettingsUiEvent.AutoSyncDelayChange -> onAutoSyncDelayChange(minutes = event.minutes)
		is SettingsUiEvent.ConnectionTimeoutChange -> onConnectionTimeoutChange(connectionTimeout = event.connectionTimeout)
	}

	private fun requestName() = watch.scope.launch {
		_uiState.update { it.copy(isNameLoading = true) }
		watch.requestName()
	}

	private fun requestWatchSettings() = watch.scope.launch {
		_uiState.update { it.copy(isWatchSettingsLoading = true) }
		watch.requestWatchSettings()
	}


	private fun requestConnectionSettings() = watch.scope.launch {
		_uiState.update { it.copy(isConnectionSettingsLoading = true) }
		watch.requestConnectionSettings()
	}

	private fun writeSettings() = watch.scope.launch {
		val isNameUpdated = _uiState.value.isNameUpdated
		val isWatchSettingsUpdated = _uiState.value.isWatchSettingsUpdated
		val isConnectionSettingsUpdated = _uiState.value.isConnectionSettingsUpdated

		_uiState.update {
			it.copy(
				isNameLoading = isNameUpdated,
				isWatchSettingsLoading = isWatchSettingsUpdated,
				isConnectionSettingsLoading = isConnectionSettingsUpdated,
			)
		}

		if (isNameUpdated) watch.writeName(_uiState.value.pendingName)
		if (isWatchSettingsUpdated) watch.writeWatchSettings(_uiState.value.pendingWatchSettings)
		if (isConnectionSettingsUpdated) watch.writeConnectionSettings(_uiState.value.pendingConnectionSettings)
	}

	private fun onNameInputChange(name: String) = _uiState.updateCatching(
		transform = { it.pendingName.copy(_value = name) },
		onSuccess = { copy(pendingName = it, pendingNameError = null) },
		onFailure = { copy(pendingNameError = it) },
	)

	private fun onIs24HourChange(is24Hour: Boolean) = _uiState.updateCatching(
		transform = {
			it.pendingWatchSettings.copy(
				preferences = it.pendingWatchSettings.preferences.copy(
					is24HourTime = is24Hour
				)
			)
		},
		onSuccess = { copy(pendingWatchSettings = it, pendingWatchSettingsError = null) },
		onFailure = { copy(pendingWatchSettingsError = it) },
	)

	private fun onIsMutedChange(isMuted: Boolean) = _uiState.updateCatching(
		transform = {
			it.pendingWatchSettings.copy(
				preferences = it.pendingWatchSettings.preferences.copy(
					isToneMuted = isMuted
				)
			)
		},
		onSuccess = { copy(pendingWatchSettings = it, pendingWatchSettingsError = null) },
		onFailure = { copy(pendingWatchSettingsError = it) },
	)

	private fun onAutoBacklightChange(enable: Boolean) = _uiState.updateCatching(
		transform = {
			it.pendingWatchSettings.copy(
				preferences = it.pendingWatchSettings.preferences.copy(
					autoBacklight = enable
				)
			)
		},
		onSuccess = { copy(pendingWatchSettings = it, pendingWatchSettingsError = null) },
		onFailure = { copy(pendingWatchSettingsError = it) },
	)

	private fun onPowerSavingChange(enable: Boolean) = _uiState.updateCatching(
		transform = {
			it.pendingWatchSettings.copy(
				preferences = it.pendingWatchSettings.preferences.copy(
					powerSaving = enable
				)
			)
		},
		onSuccess = { copy(pendingWatchSettings = it, pendingWatchSettingsError = null) },
		onFailure = { copy(pendingWatchSettingsError = it) },
	)

	private fun onBacklightDurationChange(backlightDuration: BacklightDuration) =
		_uiState.updateCatching(
			transform = { it.pendingWatchSettings.copy(backlightDuration = backlightDuration) },
			onSuccess = { copy(pendingWatchSettings = it, pendingWatchSettingsError = null) },
			onFailure = { copy(pendingWatchSettingsError = it) },
		)

	private fun onDateFormatChange(dateFormat: DateFormat) = _uiState.updateCatching(
		transform = { it.pendingWatchSettings.copy(dateFormat = dateFormat) },
		onSuccess = { copy(pendingWatchSettings = it, pendingWatchSettingsError = null) },
		onFailure = { copy(pendingWatchSettingsError = it) },
	)

	private fun onWeekdayLanguageChange(weekdayLanguage: WeekdayLanguage) = _uiState.updateCatching(
		transform = { it.pendingWatchSettings.copy(weekdayLanguage = weekdayLanguage) },
		onSuccess = { copy(pendingWatchSettings = it, pendingWatchSettingsError = null) },
		onFailure = { copy(pendingWatchSettingsError = it) },
	)

	private fun onAutoSyncChange(enable: Boolean) = _uiState.updateCatching(
		transform = { it.pendingConnectionSettings.copy(autoSyncEnable = enable) },
		onSuccess = { copy(pendingConnectionSettings = it, pendingConnectionSettingsError = null) },
		onFailure = { copy(pendingConnectionSettingsError = it) },
	)

	private fun onAutoSyncDelayChange(minutes: Int) = _uiState.updateCatching(
		transform = { it.pendingConnectionSettings.copy(autoSyncDelay = AutoSyncDelay(minutes)) },
		onSuccess = { copy(pendingConnectionSettings = it, pendingConnectionSettingsError = null) },
		onFailure = { copy(pendingConnectionSettingsError = it) },
	)

	private fun onConnectionTimeoutChange(connectionTimeout: ConnectionTimeout) =
		_uiState.updateCatching(
			transform = { it.pendingConnectionSettings.copy(connectionTimeout = connectionTimeout) },
			onSuccess = {
				copy(pendingConnectionSettings = it, pendingConnectionSettingsError = null)
			},
			onFailure = { copy(pendingConnectionSettingsError = it) },
		)
}

private val defaultWatchName = WatchName("")
private val defaultWatchSettings = WatchSettings(
	preferences = WatchPreferences(
		is24HourTime = false,
		isToneMuted = false,
		autoBacklight = false,
		powerSaving = false
	),
	backlightDuration = BacklightDuration.SHORT,
	dateFormat = DateFormat.MMDD,
	weekdayLanguage = WeekdayLanguage.EN
)
private val defaultConnectionSettings = ConnectionSettings(
	autoSyncEnable = false,
	autoSyncDelay = AutoSyncDelay(minutes = 30),
	connectionTimeout = ConnectionTimeout.MINUTES_5
)