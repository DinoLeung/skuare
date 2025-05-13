package xyz.d1n0.ui.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Watch
import com.juul.kable.State as PeripheralState

class NavBarViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()

	val watchState: StateFlow<PeripheralState> get() = watch.state

	private val _disconnectEvents = MutableSharedFlow<Unit>()
	val disconnectEvents: SharedFlow<Unit> = _disconnectEvents.asSharedFlow()

	init {
		watchState
			.filterIsInstance<PeripheralState.Disconnected>()
			.onEach { _disconnectEvents.emit(Unit) }
			.launchIn(viewModelScope)
	}
}