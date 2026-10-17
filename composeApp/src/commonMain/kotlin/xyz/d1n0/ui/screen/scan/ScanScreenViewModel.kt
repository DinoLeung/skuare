package xyz.d1n0.ui.screen.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_CONNECT
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_SCAN
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.sync.WatchSessionHandler

data class ScanUiState(
	val isScanning: Boolean = false,
	val scanPermissionState: PermissionState = PermissionState.NotDetermined,
	val connectPermissionState: PermissionState = PermissionState.NotDetermined,
)

sealed interface ScanUiEvent {
	object Scan : ScanUiEvent
	object StopScanning : ScanUiEvent
	object RequestPermissions : ScanUiEvent
	data class Connect(val peripheral: Peripheral) : ScanUiEvent
}

class ScanScreenViewModel(
	val onConnect: () -> Unit,
	val permissionsController: PermissionsController,
) : ViewModel(), KoinComponent {
	val log: Log by inject()
	private val watchConnectionManager: WatchConnectionManager by inject()
	private val watchSessionHandler: WatchSessionHandler by inject()

	private val _state = MutableStateFlow(ScanUiState())
	val state: StateFlow<ScanUiState> = _state.asStateFlow()

	private var scanJob: Job? = null

	init {
		// a watch connected in background after a lower-left long press, the app was opened for it
		viewModelScope.launch {
			watchSessionHandler.pendingForegroundWatch.filterNotNull().collect {
				watchSessionHandler.takePendingForegroundWatch()?.let { watch ->
					stopScanning()
					watchSessionHandler.runForeground(watch)
					onConnect()
				}
			}
		}
	}

	fun onEvent(event: ScanUiEvent) = when (event) {
		ScanUiEvent.RequestPermissions -> checkScanPermissions()
		ScanUiEvent.Scan -> startScanning()
		ScanUiEvent.StopScanning -> stopScanning()
		is ScanUiEvent.Connect -> viewModelScope.launch { connect(event.peripheral) }
	}

	private fun startScanning() {
		scanJob = viewModelScope.launch {
			_state.update { it.copy(isScanning = true) }
			// TODO: what if there are more than 1 peripheral found
			Watch.scanner.advertisements.firstOrNull()?.let {
				connect(Peripheral(it))
				stopScanning()
			}
		}
	}

	private suspend fun connect(peripheral: Peripheral) {
		val watch = watchConnectionManager.connect(peripheral)
		watchSessionHandler.runForeground(watch)
		onConnect()
	}

	private fun stopScanning() {
		scanJob?.cancel().also {
			_state.update { it.copy(isScanning = false) }
		}
	}

//    permissionsController.providePermission(Permission.BLUETOOTH_LE)
//    permissionsController.providePermission(Permission.BACKGROUND_LOCATION)

	private fun checkScanPermissions() = viewModelScope.launch {
		_state.update {
			it.copy(
				scanPermissionState = permissionsController.getPermissionState(Permission.BLUETOOTH_SCAN),
				connectPermissionState = permissionsController.getPermissionState(Permission.BLUETOOTH_CONNECT)
			)
		}
		if (_state.value.scanPermissionState != PermissionState.Granted) requestScanPermission()
		if (_state.value.connectPermissionState != PermissionState.Granted) requestConnectionPermission()
	}

	private suspend fun requestScanPermission() = runCatching {
		if (state.value.scanPermissionState != PermissionState.DeniedAlways)
			permissionsController.providePermission(Permission.BLUETOOTH_SCAN)
	}.onSuccess {
		_state.value = _state.value.copy(scanPermissionState = PermissionState.Granted)
	}.onFailure {
		val newPermissionState = when (it) {
			is DeniedAlwaysException -> PermissionState.DeniedAlways
			is DeniedException -> PermissionState.Denied
			is RequestCanceledException -> PermissionState.NotDetermined
			else -> PermissionState.NotDetermined
		}
		_state.value = _state.value.copy(scanPermissionState = newPermissionState)
	}

	private suspend fun requestConnectionPermission() = runCatching {
		if (state.value.connectPermissionState != PermissionState.DeniedAlways)
			permissionsController.providePermission(Permission.BLUETOOTH_CONNECT)
	}.onSuccess {
		_state.value = _state.value.copy(connectPermissionState = PermissionState.Granted)
	}.onFailure {
		val newPermissionState = when (it) {
			is DeniedAlwaysException -> PermissionState.DeniedAlways
			is DeniedException -> PermissionState.Denied
			is RequestCanceledException -> PermissionState.NotDetermined
			else -> PermissionState.NotDetermined
		}
		_state.value = _state.value.copy(connectPermissionState = newPermissionState)
	}
}