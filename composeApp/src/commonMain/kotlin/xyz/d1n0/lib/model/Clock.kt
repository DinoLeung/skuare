package xyz.d1n0.lib.model

import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.helper.toByteArray
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock as KotlinClock
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import kotlinx.datetime.TimeZone as KotlinTimeZone

/**
 * A clock on the watch. The subclass says which lookup table the time zone comes from,
 * [HomeClock] for the home table and [WorldClock] for the world table, not which slot it is in.
 * The watch can swap them, so the pinned slot can hold a [WorldClock] and the other slots a [HomeClock].
 */
sealed class Clock {
	abstract val timeZone: Timezone
	abstract val dstSettings: DstSettings

	companion object {
		/**
		 * Home time zone identifiers start at this value; world time zone identifiers are all below it.
		 */
		const val HOME_TIMEZONE_ID_START = 30000

		/**
		 * Creates a [HomeClock] or a [WorldClock] depending on which table [timeZoneId] belongs to.
		 *
		 * @throws IllegalArgumentException If the time zone ID exists in neither table.
		 */
		fun fromTimeZoneId(timeZoneId: Short, dstSettings: DstSettings): Clock =
			if (timeZoneId >= HOME_TIMEZONE_ID_START) HomeClock.fromTimeZoneId(timeZoneId, dstSettings)
			else WorldClock.fromTimeZoneId(timeZoneId, dstSettings)
	}

	fun ianaOffsetString(): String = timeZone.ianaOffsetString()

	/**
	 * Returns the current local date and time, optionally adjusted by a delay.
	 *
	 * Uses the system clock to obtain the current time, applies an optional delay,
	 * and converts the result into a [KotlinLocalDateTime] in the clock's time zone.
	 *
	 * @param delay An optional duration to shift the current time forward (default is no delay).
	 * @return The adjusted local date and time.
	 */
	fun getCurrentDateTime(delay: Duration = 0.seconds): KotlinLocalDateTime {
		val timeZone = KotlinTimeZone.of(timeZone.timeZone)
		val now = KotlinClock.System.now() + delay
		return now.toLocalDateTime(timeZone)
	}

	/**
	 * Creates a byte array representing the current date and time packet.
	 *
	 * The packet includes:
	 * - A command identifier byte
	 * - The current date and time (optionally delayed) as a byte array
	 * - A status byte
	 *
	 * @param delay An optional duration to shift the current time forward (default is no delay).
	 * @return A [ByteArray] formatted for transmission.
	 */
	fun getCurrentDateTimePacket(delay: Duration = 0.seconds) = byteArrayOf(
		OpCode.CURRENT_TIME.byte, *getCurrentDateTime(delay).toByteArray(), 0x01.toByte()
	)
}
