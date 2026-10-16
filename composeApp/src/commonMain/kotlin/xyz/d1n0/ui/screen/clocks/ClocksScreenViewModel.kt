package xyz.d1n0.ui.screen.clocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.helper.replaceAt
import xyz.d1n0.lib.model.ClocksSettings
import xyz.d1n0.lib.model.HomeTimezone
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.WorldTimezone
import xyz.d1n0.lib.model.endClockUpdateSession
import xyz.d1n0.lib.model.requestClocks
import xyz.d1n0.lib.model.startClockUpdateSession
import xyz.d1n0.lib.model.writeClocks
import xyz.d1n0.lib.model.writeTimeZoneConfigs
import xyz.d1n0.lib.model.writeTimeZoneCoordinatesAndRadioId
import xyz.d1n0.lib.model.writeTimeZoneNames
import xyz.d1n0.ui.boilerplate.updateCatching

data class ClocksUiState(
	val isLoading: Boolean = true,
	val savedClocks: ClocksSettings = ClocksSettings(),
	val pendingClocks: ClocksSettings = ClocksSettings(),
	val pendingHomeClockError: Throwable? = null,
	val pendingWorldClockErrors: List<Throwable?> = List(5) { null },
) {
	val isInitialized: Boolean
		get() = savedClocks.isInitialized

	val hasUpdates: Boolean
		get() = savedClocks != pendingClocks

	val hasErrors: Boolean
		get() = pendingHomeClockError != null || pendingWorldClockErrors.any { it != null }
}

sealed interface ClocksUiEvent {
	object RequestClocks : ClocksUiEvent
	object SaveClocks : ClocksUiEvent
	data class HomeTimeZoneChange(val timeZone: HomeTimezone) : ClocksUiEvent
	data class WorldTimeZoneChange(val index: Int, val timeZone: WorldTimezone) : ClocksUiEvent
}

class ClocksScreenViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val clocks: StateFlow<ClocksSettings> = watch.clocks

	private val _uiState = MutableStateFlow(ClocksUiState())
	val uiState: StateFlow<ClocksUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			clocks.collect { fetchedClocks ->
				_uiState.updateCatching(
					transform = {
						it.copy(
							savedClocks = fetchedClocks,
							pendingClocks = if (
								it.isInitialized == false || it.hasUpdates == false
							) fetchedClocks else it.pendingClocks,
						)
					},
					onSuccess = {
						it.copy(
							isLoading = fetchedClocks.isInitialized == false ||
								fetchedClocks != it.pendingClocks,
						)
					},
					onFailure = { copy() },
				)
			}
		}
	}

	fun onEvent(event: ClocksUiEvent) = when (event) {
		ClocksUiEvent.RequestClocks -> requestClocks()
		ClocksUiEvent.SaveClocks -> writeClocks()
		is ClocksUiEvent.HomeTimeZoneChange -> onHomeTimeZoneChange(event.timeZone)
		is ClocksUiEvent.WorldTimeZoneChange -> onWorldTimeZoneChange(
			index = event.index,
			timeZone = event.timeZone,
		)
	}

	private fun requestClocks() = watch.scope.launch {
		_uiState.update { it.copy(isLoading = true) }
		watch.requestClocks()
	}

	private fun writeClocks() = watch.scope.launch {
		_uiState.update { it.copy(isLoading = true) }
		val pendingClocks = _uiState.value.pendingClocks

		watch.startClockUpdateSession()
		watch.writeClocks(pendingClocks)
		watch.writeTimeZoneConfigs(pendingClocks)
		watch.writeTimeZoneNames(pendingClocks)
		watch.writeTimeZoneCoordinatesAndRadioId(pendingClocks)
		watch.endClockUpdateSession()

		watch.requestClocks()

	}

	private fun onHomeTimeZoneChange(timeZone: HomeTimezone) = _uiState.updateCatching(
		transform = {
			requireNotNull(it.pendingClocks.homeClock) { "Home clock is not initialized" }
				.copy(timeZone = timeZone)
		},
		onSuccess = {
			copy(
				pendingClocks = pendingClocks.copy(homeClock = it),
				pendingHomeClockError = null,
			)
		},
		onFailure = { copy(pendingHomeClockError = it) },
	)

	private fun onWorldTimeZoneChange(index: Int, timeZone: WorldTimezone) =
		_uiState.updateCatching(
			transform = {
				requireNotNull(it.pendingClocks.worldClocks[index]) {
					"World clock at index $index is not initialized"
				}.copy(timeZone = timeZone)
			},
			onSuccess = {
				copy(
					pendingClocks = pendingClocks.copy(
						worldClocks = pendingClocks.worldClocks.replaceAt(
							index = index,
							with = it,
						),
					),
					pendingWorldClockErrors = pendingWorldClockErrors.replaceAt(index, null),
				)
			},
			onFailure = {
				copy(
					pendingWorldClockErrors = pendingWorldClockErrors.replaceAt(index, it),
				)
			},
		)
}
