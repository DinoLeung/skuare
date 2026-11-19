package xyz.d1n0.lib.constant

import kotlinx.datetime.LocalTime
import xyz.d1n0.lib.model.ValueOutOfRangeException
import kotlin.jvm.JvmInline

/**
 * Minutes past the hour at which the watch connects to adjust its time, on each of
 * [ADJUSTMENT_HOURS]. A delay of 30 adjusts at 00:30, 06:30, 12:30 and 18:30.
 */
@JvmInline
value class AutoSyncDelay(val minutes: Int) {
	init {
		if (minutes !in RANGE) {
			throw ValueOutOfRangeException(RANGE, "AutoSyncDelay must be in $RANGE, got $minutes")
		}
	}

	val byte: Byte
		get() = minutes.toByte()

	/** Watch times at which the auto time adjustment runs, earliest first. */
	val adjustmentTimes: List<LocalTime>
		get() = ADJUSTMENT_HOURS.map { LocalTime(it, minutes) }

	companion object {
		val RANGE = 0..59

		/** The watch runs the auto time adjustment four times a day, at these hours. */
		val ADJUSTMENT_HOURS = listOf(0, 6, 12, 18)

		fun fromByte(byte: Byte): AutoSyncDelay =
			AutoSyncDelay(byte.toInt())
	}

	override fun toString(): String {
		return minutes.toString()
	}
}
