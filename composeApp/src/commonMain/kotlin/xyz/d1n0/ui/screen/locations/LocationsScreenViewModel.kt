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
import kotlin.time.Instant

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

	/**
	 * The last single delete, until it's undone or a later delete supersedes it. Only it can be
	 * undone: a bulk delete the user confirmed since must not bring it back.
	 */
	private var undoable: PressedLocation? = null

	/** Deletes one location, [restore] puts it back, e.g. from the snackbar's Undo. */
	fun delete(location: PressedLocation) {
		undoable = location
		viewModelScope.launch { pressedLocationRepository.delete(location) }
	}

	/** Undoes [delete], if [location] is still the last one deleted and no bulk delete followed. */
	fun restore(location: PressedLocation) {
		if (undoable?.id != location.id) return
		undoable = null
		viewModelScope.launch { pressedLocationRepository.restore(location) }
	}

	/** Names [location], a blank [name] goes back to the watch's name. */
	fun rename(location: PressedLocation, name: String) {
		viewModelScope.launch { pressedLocationRepository.rename(location, name) }
	}

	/** What each [LocationAge] would delete of the loaded locations, for its dialog. */
	fun olderThan(): Map<LocationAge, OlderThan> {
		val now = clock.now()
		val zone = timeZone()
		val locations = state.value.locations.orEmpty()
		return LocationAge.entries.associateWith { age ->
			// in whole milliseconds, like the database stores it (Converters), otherwise a location in
			// the cutoff's millisecond is counted here but not deleted by the query
			val cutoff = age.cutoff(now, zone).toEpochMilliseconds().let(Instant::fromEpochMilliseconds)
			OlderThan(cutoff = cutoff, count = locations.count { it.location.pressedAt < cutoff })
		}
	}

	/** Deletes the locations before [cutoff], the one [olderThan] counted with. */
	fun deleteBefore(cutoff: Instant) {
		undoable = null
		viewModelScope.launch { pressedLocationRepository.deleteBefore(cutoff) }
	}

	fun deleteAll() {
		undoable = null
		viewModelScope.launch { pressedLocationRepository.deleteAll() }
	}
}
