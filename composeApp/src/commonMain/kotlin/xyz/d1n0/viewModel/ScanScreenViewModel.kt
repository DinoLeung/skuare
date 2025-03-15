package xyz.d1n0.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xyz.d1n0.Repo
import xyz.d1n0.model.Watch


data class ScanScreenState(
    val scanJob: Job? = null,
    val isScanning: Boolean = false,
)

class ScanScreenViewModel(private val repo: Repo): ViewModel() {
    private val _state = MutableStateFlow(ScanScreenState())
    val state: StateFlow<ScanScreenState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScanning(onWatchFound: (Watch) -> Unit) {
        scanJob = viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            Watch.scanner.advertisements.firstOrNull()?.let {
                val watch = Watch(Peripheral(it))
                repo.setWatch(watch)

                onWatchFound(watch)
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