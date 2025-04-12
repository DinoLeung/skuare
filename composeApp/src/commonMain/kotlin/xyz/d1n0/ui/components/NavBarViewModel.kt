package xyz.d1n0.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Watch
import com.juul.kable.State as PeripheralState

class NavBarViewModel: ViewModel(), KoinComponent {
    private val watch: Watch by inject()

    val watchState: StateFlow<PeripheralState> get() = watch.state

    fun connect(onConnectionLost: () -> Unit) = viewModelScope.launch {
        watch.connect()
            .invokeOnCompletion {
                viewModelScope.launch(Dispatchers.Main) { onConnectionLost() }
            }
    }
}