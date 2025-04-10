package xyz.d1n0.ui.screen.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
import dev.icerock.moko.permissions.*
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_SCAN
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.model.Watch

data class ScanScreenState(
    val isScanning: Boolean = false,
    val hasScanPermission: Boolean = false,
    val scanPermissionState:PermissionState = PermissionState.NotDetermined,
)

class ScanScreenViewModel: ViewModel(), KoinComponent {
    val log: Log by inject()
    val permissionsController: PermissionsController by inject()

    private val _state = MutableStateFlow(ScanScreenState())
    val state: StateFlow<ScanScreenState> = _state.asStateFlow()
    private var scanJob: Job? = null

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

//    permissionsController.providePermission(Permission.BLUETOOTH_LE)
//    permissionsController.providePermission(Permission.BACKGROUND_LOCATION)

    fun checkScanPermissions() = viewModelScope.launch {
        val isGranted = permissionsController.isPermissionGranted(Permission.BLUETOOTH_SCAN)
        _state.value = _state.value.copy(hasScanPermission = isGranted)
        if (isGranted) return@launch

        runCatching {
            if (state.value.hasScanPermission == false
                && state.value.scanPermissionState != PermissionState.DeniedAlways) {
                permissionsController.providePermission(Permission.BLUETOOTH_SCAN)
                _state.value = _state.value.copy(
                    hasScanPermission = true,
                    scanPermissionState = PermissionState.Granted,
                )
            }
        }.onFailure {
            when (it) {
                is DeniedAlwaysException -> {
                    _state.value = _state.value.copy(
                        hasScanPermission = false,
                        scanPermissionState = PermissionState.DeniedAlways,
                    )
                }
                is DeniedException -> {
                    _state.value = _state.value.copy(
                        hasScanPermission = false,
                        scanPermissionState = PermissionState.Denied,
                    )
                }
                is RequestCanceledException -> {
                    _state.value = _state.value.copy(hasScanPermission = false)
                    log.e { "Request was canceled: ${it.stackTraceToString()}" }
                }
            }
        }
    }
}