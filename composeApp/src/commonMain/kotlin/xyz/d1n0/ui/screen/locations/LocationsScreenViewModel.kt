package xyz.d1n0.ui.screen.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import xyz.d1n0.data.database.PressedLocation
import xyz.d1n0.data.database.PressedLocationWithWatch
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.data.repository.PressedLocationRepository
import kotlin.time.Clock

/**
 * @property locations Newest first, null until loaded, so the empty text doesn't flash.
 */
data class LocationsUiState(
	val locations: List<PressedLocationWithWatch>? = null,
	val isLocationRecordingEnabled: Boolean = false,
)

/**
 * @param clock and [timeZone] decide what "older than" a [LocationAge] is, fixed in tests.
 */
class LocationsScreenViewModel(
	private val pressedLocationRepository: PressedLocationRepository,
	appSettingsRepository: AppSettingsRepository,
	private val clock: Clock = Clock.System,
	private val timeZone: () -> TimeZone = TimeZone::currentSystemDefault,
) : ViewModel() {
	val state: StateFlow<LocationsUiState> = combine(
		pressedLocationRepository.observeAll(),
		appSettingsRepository.observeLocationRecordingEnabled(),
	) { locations, enabled -> LocationsUiState(locations, enabled) }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationsUiState())

	/** Deletes one location, [restore] puts it back, e.g. from the snackbar's Undo. */
	fun delete(location: PressedLocation) {
		viewModelScope.launch { pressedLocationRepository.delete(location) }
	}

	fun restore(location: PressedLocation) {
		viewModelScope.launch { pressedLocationRepository.restore(location) }
	}

	/** How many of the loaded locations each [LocationAge] would delete, for its dialog. */
	fun countsOlderThan(): Map<LocationAge, Int> {
		val now = clock.now()
		val zone = timeZone()
		val locations = state.value.locations.orEmpty()
		return LocationAge.entries.associateWith { age ->
			val cutoff = age.cutoff(now, zone)
			locations.count { it.location.pressedAt < cutoff }
		}
	}

	fun deleteOlderThan(age: LocationAge) {
		val cutoff = age.cutoff(clock.now(), timeZone())
		viewModelScope.launch { pressedLocationRepository.deleteBefore(cutoff) }
	}

	fun deleteAll() {
		viewModelScope.launch { pressedLocationRepository.deleteAll() }
	}
}
