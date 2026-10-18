package xyz.d1n0.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Watch
import com.juul.kable.State as PeripheralState

class NavBarViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()

	val watchState: StateFlow<PeripheralState> get() = watch.state

	/**
	 * Tabs for the connected watch, without the ones for features it doesn't have.
	 */
	val navBarItems: List<NavBarItem> = navBarItemsFor(watch.model)

	private val _disconnectEvents = MutableSharedFlow<Unit>()
	val disconnectEvents: SharedFlow<Unit> = _disconnectEvents.asSharedFlow()

	private val _isSaving = MutableStateFlow(false)
	val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

	init {
		watchState
			.filterIsInstance<PeripheralState.Disconnected>()
			.onEach { _disconnectEvents.emit(Unit) }
			.launchIn(viewModelScope)
	}

	/**
	 * Saves every screen with pending changes one after another, so write sessions
	 * (e.g. the 0x21 clock update session) never interleave with other writes.
	 */
	fun saveAll(screens: Collection<SaveableScreen>) {
		if (_isSaving.value) return
		_isSaving.value = true

		watch.scope.launch {
			try {
				screens
					.filter { it.saveState.value.canSave }
					.forEach { it.save() }
			} finally {
				_isSaving.value = false
			}
		}
	}
}
