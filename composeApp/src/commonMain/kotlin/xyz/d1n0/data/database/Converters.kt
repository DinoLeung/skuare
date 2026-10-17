package xyz.d1n0.data.database

import androidx.room.TypeConverter
import kotlin.time.Instant

class Converters {
	@TypeConverter
	fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

	@TypeConverter
	fun toInstant(epochMilliseconds: Long?): Instant? = epochMilliseconds?.let { Instant.fromEpochMilliseconds(it) }
}
