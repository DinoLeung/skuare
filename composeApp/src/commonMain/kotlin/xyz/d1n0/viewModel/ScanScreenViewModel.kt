package xyz.d1n0.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xyz.d1n0.model.Watch

data class ScanScreenState(
    val scanJob: Job? = null,
    val isScanning: Boolean = false,
)

class ScanScreenViewModel: ViewModel() {

    private val _state = MutableStateFlow(ScanScreenState())
    val state: StateFlow<ScanScreenState> = _state.asStateFlow()

    var scanJob: Job? = null
//    var isScanning = false

    fun startScanning() {
        scanJob = viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            Watch.scanner.advertisements.firstOrNull()?.let {
                val watch = Watch(Peripheral(it))
                watch!!.connect()
                    .also { println("Watch connected") }
                // TODO: then navigate to the next screen
                val watchScreenViewModel = WatchScreenViewModel(watch)

            }
            _state.update { it.copy(isScanning = false) }
        }
    }

    fun stopScanning() {
        scanJob?.cancel().also {
            _state.update { it.copy(isScanning = false) }
        }
    }
}