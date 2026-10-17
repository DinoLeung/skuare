package xyz.d1n0.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module

const val DATABASE_NAME = "skuare.db"

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

/**
 * Provides the platform specific [RoomDatabase.Builder] for [SkuareDatabase].
 */
expect val databaseModule: Module

fun RoomDatabase.Builder<SkuareDatabase>.buildSkuareDatabase(): SkuareDatabase =
	setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO)
		.build()
