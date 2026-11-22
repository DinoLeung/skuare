package xyz.d1n0.data.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

const val DATABASE_NAME = "skuare.db"

/**
 * Never use destructive migrations (e.g. fallbackToDestructiveMigration). They wipe
 * [AppConfig.installationId], and every paired watch would then see a different app info and
 * count as unpaired. Bump [Database.version] and add a migration instead, the exported schemas in
 * composeApp/schemas are what auto migrations compare against.
 */
@Database(
	entities = [PairedWatch::class, AppConfig::class, AppSettings::class, PressedLocation::class],
	version = 8,
	autoMigrations = [
		AutoMigration(from = 1, to = 2, spec = CustomWatchModelMigration::class),
		AutoMigration(from = 2, to = 3, spec = AlarmAndLocationFeaturesMigration::class),
		AutoMigration(from = 3, to = 4, spec = HandAdjustFeatureMigration::class),
		AutoMigration(from = 4, to = 5, spec = SyncFailureMigration::class),
		// adds app_settings (no row: every setting at its default) and pressed_location, nothing to fill in
		AutoMigration(from = 5, to = 6),
		// adds the find phone settings to app_settings, off, a minute, the built-in tone
		AutoMigration(from = 6, to = 7),
		// adds the find phone full volume setting to app_settings, on, as it rang before
		AutoMigration(from = 7, to = 8),
	],
)
@TypeConverters(Converters::class)
@ConstructedBy(SkuareDatabaseConstructor::class)
abstract class SkuareDatabase : RoomDatabase() {
	abstract fun pairedWatchDao(): PairedWatchDao
	abstract fun appConfigDao(): AppConfigDao
	abstract fun appSettingsDao(): AppSettingsDao
	abstract fun pressedLocationDao(): PressedLocationDao
}

// Room generates the actual implementations
@Suppress("KotlinNoActualForExpect")
expect object SkuareDatabaseConstructor : RoomDatabaseConstructor<SkuareDatabase> {
	override fun initialize(): SkuareDatabase
}

fun RoomDatabase.Builder<SkuareDatabase>.buildSkuareDatabase(): SkuareDatabase =
	setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()
