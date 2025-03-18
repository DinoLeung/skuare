package xyz.d1n0.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.mp.KoinPlatform.getKoin
import xyz.d1n0.model.Watch


data class ScanScreenState(
    val scanJob: Job? = null,
    val isScanning: Boolean = false,
)

class ScanScreenViewModel(): ViewModel() {
    private val _state = MutableStateFlow(ScanScreenState())
    val state: StateFlow<ScanScreenState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScanning(onWatchFound: () -> Unit) {
        scanJob = viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            Watch.scanner.advertisements.firstOrNull()?.let {
                // declare watch in koin, so it knows how to inject it to other places
                getKoin().declare(Watch(Peripheral(it)))
                onWatchFound()
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