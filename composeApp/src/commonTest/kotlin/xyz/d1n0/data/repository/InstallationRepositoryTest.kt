package xyz.d1n0.data.repository

import kotlinx.coroutines.test.runTest
import xyz.d1n0.data.database.AppConfig
import xyz.d1n0.data.database.AppConfigDao
import xyz.d1n0.lib.model.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class InstallationRepositoryTest {
	private class FakeAppConfigDao : AppConfigDao {
		var config: AppConfig? = null
		override suspend fun get(): AppConfig? = config
		override suspend fun insertIfMissing(config: AppConfig) {
			if (this.config == null) this.config = config
		}
	}

	@Test
	fun generatesPairedAppInfo() = runTest {
		val appInfo = InstallationRepository(FakeAppConfigDao()).appInfo()
		assertEquals(AppInfo.ID_SIZE, appInfo.id.size)
		assertEquals(AppInfo.PAIRED_FLAG, appInfo.flag)
		assertFalse(appInfo.isReset)
	}

	@Test
	fun keepsStoredId() = runTest {
		val dao = FakeAppConfigDao()
		val first = InstallationRepository(dao).appInfo()
		// a new repository, e.g. after the app restarts, reads the stored id
		assertEquals(first, InstallationRepository(dao).appInfo())
	}

	@Test
	fun generatesDifferentIdPerInstall() = runTest {
		assertNotEquals(
			InstallationRepository(FakeAppConfigDao()).appInfo(),
			InstallationRepository(FakeAppConfigDao()).appInfo(),
		)
	}
}
