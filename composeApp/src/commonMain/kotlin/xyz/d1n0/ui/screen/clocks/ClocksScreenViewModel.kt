package xyz.d1n0.ui.screen.clocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.helper.replaceAt
import xyz.d1n0.lib.model.Clock
import xyz.d1n0.lib.model.ClocksSettings
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.canWriteClocks
import xyz.d1n0.lib.model.requestClocks
import xyz.d1n0.lib.model.writeClocksTimezones
import xyz.d1n0.ui.boilerplate.updateCatching
import xyz.d1n0.ui.navigation.SaveState
import xyz.d1n0.ui.navigation.SaveableScreen

data class ClocksUiState(
	val isLoading: Boolean = false,
	val savedClocks: ClocksSettings = ClocksSettings(),
	val pendingClocks: ClocksSettings = ClocksSettings(),
	val pendingPinnedClockError: Throwable? = null,
	val pendingClockErrors: List<Throwable?> = List(5) { null },
	/**
	 * False on watches that can't take clock writes yet, the clocks are then read-only.
	 * Temporary, goes away with [xyz.d1n0.lib.model.canWriteClocks].
	 */
	val isEditable: Boolean = true,
) {
	val isInitialized: Boolean
		get() = savedClocks.isInitialized

	val hasUpdates: Boolean
		get() = savedClocks != pendingClocks

	val hasErrors: Boolean
		get() = pendingPinnedClockError != null || pendingClockErrors.any { it != null }
}

sealed interface ClocksUiEvent {
	object RequestClocks : ClocksUiEvent
	data class PinnedClockChange(val clock: Clock) : ClocksUiEvent
	data class ClockChange(val index: Int, val clock: Clock) : ClocksUiEvent
	data class PinClock(val index: Int) : ClocksUiEvent
}

class ClocksScreenViewModel : ViewModel(), KoinComponent, SaveableScreen {
	private val watch: Watch by inject()
	private val clocks: StateFlow<ClocksSettings> = watch.clocks

	private val _uiState = MutableStateFlow(
		ClocksUiState(
			savedClocks = clocks.value,
			pendingClocks = clocks.value,
			pendingClockErrors = List(watch.model.clockCount - 1) { null },
			isEditable = watch.canWriteClocks,
		)
	)
	val uiState: StateFlow<ClocksUiState> = _uiState.asStateFlow()

	override val saveState: StateFlow<SaveState> = _uiState
		.map { SaveState(hasUpdates = it.hasUpdates, isLoading = it.isLoading, hasErrors = it.hasErrors) }
		.stateIn(viewModelScope, SharingStarted.Eagerly, SaveState())

	init {
		viewModelScope.launch {
			clocks.collect { fetchedClocks ->
				_uiState.updateCatching(
					transform = {
						it.copy(
							savedClocks = fetchedClocks,
							pendingClocks =
								if (it.isInitialized == false || it.hasUpdates == false)
									fetchedClocks
								else it.pendingClocks,
						)
					},
					onSuccess = {
						it.copy(
							isLoading = it.isLoading && (
								fetchedClocks.isInitialized == false ||
									fetchedClocks != it.pendingClocks
							),
						)
					},
					onFailure = { copy() },
				)
			}
		}
	}

	fun onEvent(event: ClocksUiEvent) = when (event) {
		ClocksUiEvent.RequestClocks -> requestClocks()
		is ClocksUiEvent.PinnedClockChange -> onPinnedClockChange(event.clock)
		is ClocksUiEvent.ClockChange -> onClockChange(
			index = event.index,
			clock = event.clock,
		)
		is ClocksUiEvent.PinClock -> onPinClock(event.index)
	}

	private fun requestClocks() = watch.scope.launch {
		_uiState.update { it.copy(isLoading = true) }
		watch.requestClocks()
	}

	override suspend fun save() {
		_uiState.update { it.copy(isLoading = true) }
		watch.writeClocksTimezones(_uiState.value.pendingClocks)
			.onFailure { _uiState.update { it.copy(isLoading = false) } }
	}

	private fun onPinnedClockChange(clock: Clock) = _uiState.updateCatching(
		transform = {
			requireNotNull(it.pendingClocks.pinnedClock) { "Pinned clock is not initialized" }
			clock
		},
		onSuccess = {
			copy(
				pendingClocks = pendingClocks.copy(pinnedClock = it),
				pendingPinnedClockError = null,
			)
		},
		onFailure = { copy(pendingPinnedClockError = it) },
	)

	private fun onPinClock(index: Int) = _uiState.updateCatching(
		transform = { it.pendingClocks.pin(index) },
		onSuccess = {
			copy(
				pendingClocks = it,
				pendingPinnedClockError = pendingClockErrors[index],
				pendingClockErrors = pendingClockErrors.replaceAt(index, pendingPinnedClockError),
			)
		},
		onFailure = {
			copy(pendingClockErrors = pendingClockErrors.replaceAt(index, it))
		},
	)

	private fun onClockChange(index: Int, clock: Clock) =
		_uiState.updateCatching(
			transform = {
				requireNotNull(it.pendingClocks.clocks[index]) {
					"Clock at index $index is not initialized"
				}
				clock
			},
			onSuccess = {
				copy(
					pendingClocks = pendingClocks.copy(
						clocks = pendingClocks.clocks.replaceAt(
							index = index,
							with = it,
						),
					),
					pendingClockErrors = pendingClockErrors.replaceAt(index, null),
				)
			},
			onFailure = {
				copy(
					pendingClockErrors = pendingClockErrors.replaceAt(index, it),
				)
			},
		)
}
