package xyz.d1n0.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_SCAN
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.model.Watch

data class ScanScreenState(
    val isScanning: Boolean = false,
)

class ScanScreenViewModel: ViewModel(), KoinComponent {
    val log: Log by inject()
    val permissionsController: PermissionsController by inject()

    private val _state = MutableStateFlow(ScanScreenState())
    val state: StateFlow<ScanScreenState> = _state.asStateFlow()
    private var scanJob: Job? = null

    private val _scanPermissionState = MutableStateFlow<PermissionState>(PermissionState.NotDetermined)
    val scanPermissionState: StateFlow<PermissionState> = _scanPermissionState.asStateFlow()

    init {
        viewModelScope.launch {
            val permissionState = permissionsController.getPermissionState(Permission.BLUETOOTH_SCAN)
            _scanPermissionState.value = permissionState
        }
    }

    fun startScanning(onWatchFound: () -> Unit) {
        scanJob = viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            // TODO: what if there are more than 1 peripheral found
            Watch.scanner.advertisements.firstOrNull()?.let {
                // declare watch in koin, so it knows how to inject it to other places
                getKoin().declare(Watch(Peripheral(it)))
            }
                .also { stopScanning() }
                .also { onWatchFound() }
        }
    }

    fun stopScanning() {
        scanJob?.cancel().also {
            _state.update { it.copy(isScanning = false) }
        }
    }
}