package xyz.d1n0.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.d1n0.data.database.AppConfig
import xyz.d1n0.data.database.AppConfigDao
import xyz.d1n0.lib.model.AppInfo
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InstallationRepository(private val dao: AppConfigDao) {
	private val mutex = Mutex()
	private var appInfo: AppInfo? = null

	/**
	 * The app info this install writes to the watches it pairs with. The id is generated on first
	 * use and stays the same until the app data is cleared.
	 */
	suspend fun appInfo(): AppInfo = mutex.withLock {
		appInfo ?: run {
			dao.insertIfMissing(AppConfig(installationId = generateInstallationId()))
			val config = checkNotNull(dao.get()) { "App config missing after insert" }
			AppInfo(id = config.installationId)
		}.also { appInfo = it }
	}

	/**
	 * [Uuid.random] is cryptographically secure. Bytes 6 and 8 hold the UUID version and variant
	 * bits, so only fully random bytes are taken.
	 */
	@OptIn(ExperimentalUuidApi::class)
	private fun generateInstallationId(): ByteArray =
		Uuid.random().toByteArray().let { it.copyOfRange(0, 6) + it.copyOfRange(12, 16) }
}
