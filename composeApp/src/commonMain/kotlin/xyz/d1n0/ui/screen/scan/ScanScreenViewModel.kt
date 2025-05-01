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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.adjustTime
import xyz.d1n0.lib.model.requestConnectReason
import com.juul.kable.State as PeripheralState

data class ScanUiState(
	val isScanning: Boolean = false,
	val scanPermissionState: PermissionState = PermissionState.NotDetermined,
	val connectPermissionState: PermissionState = PermissionState.NotDetermined,
)

sealed interface ScanUiEvent {
	object Scan : ScanUiEvent
	object StopScanning : ScanUiEvent
	object RequestPermissions : ScanUiEvent
}

class ScanScreenViewModel(
	val onConnect: () -> Unit,
) : ViewModel(), KoinComponent {
	val log: Log by inject()
	val permissionsController: PermissionsController by inject()

	private val _state = MutableStateFlow(ScanUiState())
	val state: StateFlow<ScanUiState> = _state.asStateFlow()

	fun onEvent(event: ScanUiEvent) = when (event) {
		ScanUiEvent.RequestPermissions -> checkScanPermissions()
		ScanUiEvent.Scan -> startScanning()
		ScanUiEvent.StopScanning -> stopScanning()
	}

	private var scanJob: Job? = null
	
	private fun startScanning() {
		scanJob = viewModelScope.launch {
			_state.update { it.copy(isScanning = true) }
			// TODO: what if there are more than 1 peripheral found
			Watch.scanner.advertisements.firstOrNull()?.let {
				val watch = Watch(Peripheral(it))
				// declare watch in koin, so it knows how to inject it to other places
				getKoin().declare(watch)

				watch.connect()

				watch.state.collect {
					if (it is PeripheralState.Connected) {
						watch.scope.run {
							watch.requestConnectReason()
							watch.adjustTime()
						}.also {
							onConnect()
							stopScanning()
						}
					}
				}
			}
		}
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