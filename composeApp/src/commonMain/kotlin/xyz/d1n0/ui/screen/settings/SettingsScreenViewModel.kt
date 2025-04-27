package xyz.d1n0.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

class SettingsScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val watchInfo: StateFlow<WatchInfo> = watch.info

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

	val name: StateFlow<WatchName> = watchInfo.map { it.name ?: defaultWatchName }.stateIn(
		scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = defaultWatchName
	)
	val watchSettings: StateFlow<WatchSettings> = watchInfo.map {
		it.watchSettings ?: defaultWatchSettings
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Eagerly,
		initialValue = defaultWatchSettings
	)
	val connectionSettings: StateFlow<ConnectionSettings> =
		watchInfo.map {
			it.connectionSettings ?: defaultConnectionSettings
		}.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = defaultConnectionSettings
		)

	private val nameLocal = MutableStateFlow<WatchName>(name.value)

	fun requestName() = watch.scope.launch { watch.requestName() }
	fun writeName() = watch.scope.launch {
		_waitingUpdates.update { true }
		watch.writeName(nameLocal.value)
		watch.requestName()
	}

	private val watchSettingsLocal = MutableStateFlow<WatchSettings>(watchSettings.value)

	fun requestWatchSettings() = watch.scope.launch { watch.requestWatchSettings() }
	fun writeWatchSettings() = watch.scope.launch {
		_waitingUpdates.update { true }
		watch.writeWatchSettings(watchSettingsLocal.value)
		watch.requestWatchSettings()
	}

	private val connectionSettingsLocal =
		MutableStateFlow<ConnectionSettings>(connectionSettings.value)

	fun requestConnectionSettings() = watch.scope.launch { watch.requestConnectionSettings() }
	fun writeConnectionSettings() = watch.scope.launch {
		_waitingUpdates.update { true }
		watch.writeConnectionSettings(connectionSettingsLocal.value)
		watch.requestConnectionSettings()
	}

	private val _waitingUpdates = MutableStateFlow(true)
	val waitingUpdates: StateFlow<Boolean> = _waitingUpdates.asStateFlow()

	init {
		viewModelScope.launch {
			watchInfo.collect {
				_waitingUpdates.update { false }
			}
		}
	}

	val hasUpdate = combine(
		watchInfo,
		nameLocal,
		watchSettingsLocal,
		connectionSettingsLocal
	) { watchInfo, nameLocal, watchSettingsLocal, connectionSettingsLocal ->
		watchInfo.name != nameLocal && watchInfo.watchSettings != watchSettingsLocal && watchInfo.connectionSettings != connectionSettingsLocal
	}.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false)
}