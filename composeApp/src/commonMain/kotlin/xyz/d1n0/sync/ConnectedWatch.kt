package xyz.d1n0.sync

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.model.AppInfo
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.WatchInfo
import xyz.d1n0.lib.model.adjustTime
import xyz.d1n0.lib.model.requestWatchStatus
import xyz.d1n0.lib.model.writeAppInfo

/**
 * Operations used while handling an already connected watch session.
 *
 * Keeping the session protocol separate from Kable's platform peripheral makes pairing and
 * background-sync decisions testable in common code.
 */
interface ConnectedWatch {
	val advertisedName: String?
	val info: StateFlow<WatchInfo>

	suspend fun query(opCode: OpCode): ByteArray
	suspend fun writeAppInfo(appInfo: AppInfo)
	suspend fun requestWatchStatus()
	suspend fun adjustTime(): Result<Unit>
	suspend fun disconnect()
	suspend fun awaitDisconnected()
}

internal class KableConnectedWatch(private val watch: Watch) : ConnectedWatch {
	override val advertisedName: String? get() = watch.advertisedName
	override val info: StateFlow<WatchInfo> get() = watch.info

	override suspend fun query(opCode: OpCode): ByteArray = watch.query(opCode)
	override suspend fun writeAppInfo(appInfo: AppInfo) = watch.writeAppInfo(appInfo)
	override suspend fun requestWatchStatus() {
		watch.requestWatchStatus()
	}
	override suspend fun adjustTime(): Result<Unit> = watch.adjustTime().map { }
	override suspend fun disconnect() = watch.disconnect()
	override suspend fun awaitDisconnected() {
		watch.state.first { it is com.juul.kable.State.Disconnected }
	}
}
