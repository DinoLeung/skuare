package xyz.d1n0.ui.screen.pairing

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
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
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
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.sync.WatchPresenceMonitor
import xyz.d1n0.sync.WatchSessionHandler

data class PairingUiState(
	val isSearching: Boolean = false,
	val pairedWatches: List<PairedWatch> = emptyList(),
	val scanPermissionState: PermissionState = PermissionState.NotDetermined,
	val connectPermissionState: PermissionState = PermissionState.NotDetermined,
) {
	val isPermissionGranted: Boolean
		get() = scanPermissionState == PermissionState.Granted && connectPermissionState == PermissionState.Granted
}

sealed interface PairingUiEvent {
	object Search : PairingUiEvent
	object StopSearching : PairingUiEvent
	object RequestPermissions : PairingUiEvent
	data class Connect(val peripheral: Peripheral) : PairingUiEvent
	data class Forget(val address: String) : PairingUiEvent
}

class PairingScreenViewModel(
	val onConnect: () -> Unit,
	val permissionsController: PermissionsController,
) : ViewModel(), KoinComponent {
	val log: Log by inject()
	private val watchConnectionManager: WatchConnectionManager by inject()
	private val watchSessionHandler: WatchSessionHandler by inject()
	private val pairedWatchRepository: PairedWatchRepository by inject()
	private val presenceMonitor: WatchPresenceMonitor by inject()

	private val _state = MutableStateFlow(PairingUiState())
	val state: StateFlow<PairingUiState> = _state.asStateFlow()

	private var searchJob: Job? = null
	private var hasRequestedNotificationPermission = false

	init {
		viewModelScope.launch {
			pairedWatchRepository.observeAll().collect { watches ->
				_state.update { it.copy(pairedWatches = watches) }
			}
		}

		// a watch connected in background after a lower-left long press, the app was opened for it.
		// The background session already checked pairing and adjusted the time, and the watch is in
		// WatchConnectionManager, so the watch screens can use it straight away
		viewModelScope.launch {
			watchSessionHandler.pendingForegroundWatch.filterNotNull().collect {
				watchSessionHandler.takePendingForegroundWatch()?.let {
					stopSearching()
					onConnect()
				}
			}
		}
	}

	fun onEvent(event: PairingUiEvent) = when (event) {
		PairingUiEvent.RequestPermissions -> checkPermissions()
		PairingUiEvent.Search -> startSearching()
		PairingUiEvent.StopSearching -> stopSearching()
		is PairingUiEvent.Connect -> viewModelScope.launch { connect(event.peripheral) }
		is PairingUiEvent.Forget -> viewModelScope.launch { watchSessionHandler.forget(event.address) }
	}

	private fun startSearching() {
		searchJob = viewModelScope.launch {
			_state.update { it.copy(isSearching = true) }
			// TODO: what if there are more than 1 peripheral found
			Watch.scanner.advertisements.firstOrNull()?.let {
				connect(Peripheral(it))
				stopSearching()
			}
		}
	}

	private suspend fun connect(peripheral: Peripheral) {
		val watch = watchConnectionManager.connect(peripheral)
		watchSessionHandler.runForeground(watch)
		onConnect()
	}

	private fun stopSearching() {
		searchJob?.cancel().also {
			_state.update { it.copy(isSearching = false) }
		}
	}

//    permissionsController.providePermission(Permission.BLUETOOTH_LE)
//    permissionsController.providePermission(Permission.BACKGROUND_LOCATION)

	private fun checkPermissions() = viewModelScope.launch {
		_state.update {
			it.copy(
				scanPermissionState = permissionsController.getPermissionState(Permission.BLUETOOTH_SCAN),
				connectPermissionState = permissionsController.getPermissionState(Permission.BLUETOOTH_CONNECT)
			)
		}
		if (_state.value.scanPermissionState != PermissionState.Granted) requestScanPermission()
		if (_state.value.connectPermissionState != PermissionState.Granted) requestConnectionPermission()
		// optional, only the "open skuare" prompt after a lower-left long press needs it
		if (presenceMonitor.isSupported && !hasRequestedNotificationPermission) requestNotificationPermission()
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

	/**
	 * Asks once per visit of this screen, so denying it doesn't bring the dialog back on every resume.
	 */
	private suspend fun requestNotificationPermission() {
		hasRequestedNotificationPermission = true
		val permissionState = permissionsController.getPermissionState(Permission.REMOTE_NOTIFICATION)
		if (permissionState == PermissionState.Granted || permissionState == PermissionState.DeniedAlways) return
		runCatching { permissionsController.providePermission(Permission.REMOTE_NOTIFICATION) }
			.onFailure { log.i { "Notification permission not granted: ${it.message}" } }
	}
}