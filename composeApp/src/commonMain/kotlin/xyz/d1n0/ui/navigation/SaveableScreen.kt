package xyz.d1n0.ui.navigation

import kotlinx.coroutines.flow.StateFlow

data class SaveState(
	val hasUpdates: Boolean = false,
	val isLoading: Boolean = false,
	val hasErrors: Boolean = false,
) {
	val canSave: Boolean
		get() = hasUpdates && isLoading == false && hasErrors == false
}

/**
 * Implemented by nav bar screen view models so [NavBarScaffold] can show a single save FAB
 * for unsaved changes across all tabs.
 */
interface SaveableScreen {
	val saveState: StateFlow<SaveState>

	/** Writes pending changes to the watch, suspending until the writes are sent. */
	suspend fun save()
}
