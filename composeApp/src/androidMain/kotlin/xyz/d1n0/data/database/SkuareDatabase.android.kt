package xyz.d1n0.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun databaseBuilder(context: Context): RoomDatabase.Builder<SkuareDatabase> =
	Room.databaseBuilder<SkuareDatabase>(
		context = context,
		name = context.getDatabasePath(DATABASE_NAME).absolutePath,
	)
