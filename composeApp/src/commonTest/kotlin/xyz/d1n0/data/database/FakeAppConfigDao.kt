package xyz.d1n0.data.database

/**
 * In-memory [AppConfigDao] that follows the same rules as the Room queries.
 *
 * @param installationId Starts with the row holding this id, without a row if null.
 */
internal class FakeAppConfigDao(installationId: ByteArray? = null) : AppConfigDao {
	private var config: AppConfig? = installationId?.let { AppConfig(installationId = it) }

	override suspend fun get(): AppConfig? = config

	override suspend fun insertIfMissing(config: AppConfig) {
		if (this.config == null) this.config = config
	}
}
