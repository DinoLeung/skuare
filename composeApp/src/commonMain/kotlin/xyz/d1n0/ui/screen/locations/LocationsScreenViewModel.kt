package xyz.d1n0.ui.screen.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import xyz.d1n0.data.database.PressedLocationWithWatch
import xyz.d1n0.data.repository.AppSettingsRepository
import xyz.d1n0.data.repository.PressedLocationRepository

/**
 * @property locations Newest first, null until loaded, so the empty text doesn't flash.
 */
data class LocationsUiState(
	val locations: List<PressedLocationWithWatch>? = null,
	val isLocationRecordingEnabled: Boolean = false,
)

class LocationsScreenViewModel(
	pressedLocationRepository: PressedLocationRepository,
	appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
	val state: StateFlow<LocationsUiState> = combine(
		pressedLocationRepository.observeAll(),
		appSettingsRepository.observeLocationRecordingEnabled(),
	) { locations, enabled -> LocationsUiState(locations, enabled) }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationsUiState())
}
