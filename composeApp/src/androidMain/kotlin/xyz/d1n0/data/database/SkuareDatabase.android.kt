package xyz.d1n0.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
	single<RoomDatabase.Builder<SkuareDatabase>> {
		val context = androidContext()
		Room.databaseBuilder<SkuareDatabase>(
			context = context,
			name = context.getDatabasePath(DATABASE_NAME).absolutePath,
		)
	}
}
