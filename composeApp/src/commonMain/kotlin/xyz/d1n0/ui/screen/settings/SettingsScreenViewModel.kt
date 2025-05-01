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

data class SettingsUiState(
	val waitingUpdates: Boolean = true,
	val hasUpdates: Boolean = false,
	val isNameInitialized: Boolean = false,
	val savedName: WatchName = defaultWatchName,
	val pendingName: WatchName = defaultWatchName,
	val pendingNameError: Throwable? = null,
	val isWatchSettingsInitialized: Boolean = false,
	val savedWatchSettings: WatchSettings = defaultWatchSettings,
	val pendingWatchSettings: WatchSettings = defaultWatchSettings,
	val pendingWatchSettingsError: Throwable? = null,
	val isConnectionSettingsInitialized: Boolean = false,
	val savedConnectionSettings: ConnectionSettings = defaultConnectionSettings,
	val pendingConnectionSettings: ConnectionSettings = defaultConnectionSettings,
	val pendingConnectionSettingsError: Throwable? = null,
)

sealed interface SettingsUiEvent {
	object RequestName : SettingsUiEvent
	object SaveName : SettingsUiEvent
	object RequestWatchSettings : SettingsUiEvent
	object SaveWatchSettings : SettingsUiEvent
	object RequestConnectionSettings : SettingsUiEvent
	object SaveConnectionSettings : SettingsUiEvent
}

class SettingsScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val watchInfo: StateFlow<WatchInfo> = watch.info

	private val _uiState = MutableStateFlow(SettingsUiState())
	val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			watchInfo.collect { info ->
				_uiState.update {
					it.copy(
						waitingUpdates = false,
						hasUpdates = info.name != it.pendingName ||
								info.watchSettings != it.pendingWatchSettings ||
								info.connectionSettings != it.pendingConnectionSettings,
						isNameInitialized = info.name != null,
						savedName = info.name ?: defaultWatchName,
						isWatchSettingsInitialized = info.watchSettings != null,
						savedWatchSettings = info.watchSettings ?: defaultWatchSettings,
						isConnectionSettingsInitialized = info.connectionSettings != null,
						savedConnectionSettings = info.connectionSettings
							?: defaultConnectionSettings
					)
				}
			}
		}
	}

	fun onEvent(event: SettingsUiEvent) = when (event) {
		SettingsUiEvent.RequestName -> requestName()
		SettingsUiEvent.SaveName -> writeName()
		SettingsUiEvent.RequestWatchSettings -> requestWatchSettings()
		SettingsUiEvent.SaveWatchSettings -> writeWatchSettings()
		SettingsUiEvent.RequestConnectionSettings -> requestConnectionSettings()
		SettingsUiEvent.SaveConnectionSettings -> writeConnectionSettings()
	}

	private fun requestName() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.requestName()
	}

	private fun writeName() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.writeName(_uiState.value.pendingName)
		watch.requestName()
	}

	private fun requestWatchSettings() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.requestWatchSettings()
	}

	private fun writeWatchSettings() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.writeWatchSettings(_uiState.value.pendingWatchSettings)
		watch.requestWatchSettings()
	}

	private fun requestConnectionSettings() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.requestConnectionSettings()
	}

	private fun writeConnectionSettings() = watch.scope.launch {
		_uiState.update { it.copy(waitingUpdates = true) }
		watch.writeConnectionSettings(_uiState.value.pendingConnectionSettings)
		watch.requestConnectionSettings()
	}
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
	dateFormat = DateFormat.MDD,
	weekdayLanguage = WeekdayLanguage.EN
)
private val defaultConnectionSettings = ConnectionSettings(
	autoSyncEnable = false,
	autoSyncDelay = AutoSyncDelay.MINUTE_30,
	connectionTimeout = ConnectionTimeout.MINUTES_5
)