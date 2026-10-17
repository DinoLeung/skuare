package xyz.d1n0.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The user dismissed the "open skuare" prompt, disconnects the waiting watch. [WatchSyncService]
 * then stops, as [WatchSessionHandler.pendingForegroundWatch] clears.
 *
 * A broadcast rather than a service start, receivers have no background start restrictions.
 */
class OpenAppPromptDismissedReceiver : BroadcastReceiver(), KoinComponent {
	private val watchSessionHandler: WatchSessionHandler by inject()

	override fun onReceive(context: Context, intent: Intent) {
		val pendingResult = goAsync()
		CoroutineScope(Dispatchers.Default).launch {
			try {
				watchSessionHandler.dropPendingForegroundWatch()
			} finally {
				pendingResult.finish()
			}
		}
	}
}
