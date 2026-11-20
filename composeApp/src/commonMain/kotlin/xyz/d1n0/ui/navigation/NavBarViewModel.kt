package xyz.d1n0.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.data.database.PairedWatch
import xyz.d1n0.data.repository.PairedWatchRepository
import xyz.d1n0.lib.connection.WatchConnectionManager
import xyz.d1n0.lib.model.CustomWatchModel
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.WatchErrorPacket
import com.juul.kable.State as PeripheralState

class NavBarViewModel : ViewModel(), KoinComponent {
	private val watch: Watch by inject()
	private val pairedWatchRepository: PairedWatchRepository by inject()
	private val connectionManager: WatchConnectionManager by inject()
	private val log: Log by inject()

	val watchState: StateFlow<PeripheralState> get() = watch.state

	/**
	 * Tabs for the connected watch, without the ones for features it doesn't have.
	 */
	val navBarItems: List<NavBarItem> = navBarItemsFor(watch.model)

	/** Name to show for the watch's model, e.g. CASIO GA-B2100, null if it didn't advertise one. */
	val watchName: String? = watch.advertisedName

	/** Whether this is the demo watch, see [Watch.demo]. */
	val isDemo: Boolean = watch.isDemo

	private val _disconnectEvents = MutableSharedFlow<Unit>()
	val disconnectEvents: SharedFlow<Unit> = _disconnectEvents.asSharedFlow()

	private val _isSaving = MutableStateFlow(false)
	val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

	// known by now, the watch screens only open after the session read it
	private val address: String? = watch.info.value.address

	// a known watch's features are fixed, it never has a custom model to show
	private val pairedWatch: StateFlow<PairedWatch?> =
		(address?.takeIf { watch.model is CustomWatchModel }?.let(pairedWatchRepository::observe) ?: flowOf(null))
			.stateIn(viewModelScope, SharingStarted.Eagerly, null)

	/**
	 * The custom model of an unrecognised watch, null for a known watch, which can't be adjusted.
	 * The demo watch isn't stored, its custom model is the one it uses.
	 */
	val customModel: StateFlow<CustomWatchModel?> =
		if (isDemo) MutableStateFlow(watch.model as? CustomWatchModel).asStateFlow()
		else pairedWatch
			.map { it?.customModel }
			.stateIn(viewModelScope, SharingStarted.Eagerly, null)

	/**
	 * Whether to explain [customModel], until the user dismisses it once.
	 */
	val showCustomModelIntro: StateFlow<Boolean> = pairedWatch
		.map { it?.customModel != null && !it.customModelIntroShown }
		.stateIn(viewModelScope, SharingStarted.Eagerly, false)

	private val _reloadEvents = MutableSharedFlow<Unit>()

	/**
	 * Emitted after a custom model was saved, the watch screens must be rebuilt for its features.
	 */
	val reloadEvents: SharedFlow<Unit> = _reloadEvents.asSharedFlow()

	private val _watchError = MutableStateFlow<WatchErrorPacket?>(null)

	/**
	 * First error the watch replied with, until the user dismisses it. Later ones are dropped
	 * meanwhile, one failed read often errors on every request, e.g. FF 81 1D for each 1D.
	 */
	val watchError: StateFlow<WatchErrorPacket?> = _watchError.asStateFlow()

	init {
		watchState
			.filterIsInstance<PeripheralState.Disconnected>()
			.onEach { _disconnectEvents.emit(Unit) }
			.launchIn(viewModelScope)

		watch.errors
			.onEach { error -> _watchError.update { it ?: error } }
			.launchIn(viewModelScope)
	}

	fun dismissWatchError() {
		_watchError.value = null
	}

	/** Disconnects the watch, [disconnectEvents] follows once it is disconnected. */
	fun disconnect() {
		viewModelScope.launch { connectionManager.disconnect() }
	}

	/**
	 * Saves every screen with pending changes one after another, so write sessions
	 * (e.g. the 0x21 clock update session) never interleave with other writes.
	 */
	fun saveAll(screens: Collection<SaveableScreen>) {
		if (_isSaving.value) return
		_isSaving.value = true

		watch.scope.launch {
			try {
				screens
					.filter { it.saveState.value.canSave }
					.forEach { it.save() }
			} finally {
				_isSaving.value = false
			}
		}
	}

	fun dismissCustomModelIntro() {
		val address = address ?: return
		viewModelScope.launch { pairedWatchRepository.markCustomModelIntroShown(address) }
	}

	/**
	 * Stores [model], switches the watch to it, then asks for the watch screens to be rebuilt,
	 * which discards their unsaved changes. Does nothing while the screens are saving.
	 *
	 * @param disconnect Disconnect instead of rebuilding the screens, [model] is then used once
	 *        the watch reconnects. For when the user came from a [watchError], the watch can
	 *        keep refusing on this connection.
	 *
	 * The demo watch has nothing stored, it only switches to [model].
	 */
	fun saveCustomModel(model: CustomWatchModel, disconnect: Boolean = false) {
		if (_isSaving.value) return
		if (isDemo) {
			viewModelScope.launch {
				watch.useCustomModel(model)
				_reloadEvents.emit(Unit)
			}
			return
		}
		val address = address ?: return
		viewModelScope.launch {
			if (!pairedWatchRepository.updateCustomModel(address, model)) {
				log.w { "Custom model not stored, $address isn't paired as custom" }
				return@launch
			}
			if (disconnect) {
				log.i { "Disconnecting to recover from a watch error, $model is used on reconnect" }
				connectionManager.disconnect()
				return@launch
			}
			watch.useCustomModel(model)
			// last, rebuilding the screens clears this view model
			_reloadEvents.emit(Unit)
		}
	}
}
