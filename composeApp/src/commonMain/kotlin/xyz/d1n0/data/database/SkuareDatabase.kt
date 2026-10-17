package xyz.d1n0.data.database

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
@Database(entities = [PairedWatch::class, AppConfig::class], version = 1)
@TypeConverters(Converters::class)
@ConstructedBy(SkuareDatabaseConstructor::class)
abstract class SkuareDatabase : RoomDatabase() {
	abstract fun pairedWatchDao(): PairedWatchDao
	abstract fun appConfigDao(): AppConfigDao
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
