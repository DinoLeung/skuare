package xyz.d1n0.lib.model

import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlinx.datetime.offsetAt
import xyz.d1n0.lib.constant.timezoneRadioIdData
import xyz.d1n0.lib.helper.toLittleEndianByteArray
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock as KotlinClock
import kotlin.time.Instant

abstract class Timezone {
	abstract val timeZone: String
	abstract val cityName: String

	/**
	 * The 3-letter IATA city code, e.g. TYO for Tokyo, null if the city has none. The GW-BX5600
	 * stores it as the short city name next to the full one.
	 */
	abstract val cityCode: String?
	abstract val identifier: Short
	abstract val offset: Double
	abstract val dstDiff: Double
	abstract val dstRules: Byte

	/**
	 * Converts the identifier integer into a little-endian 2-byte array.
	 */
	val identifierBytes: ByteArray
		get() = identifier.toLittleEndianByteArray()

	/**
	 * Represents a 5-byte array constructed from components of the TimeZone object.
	 * The array includes a 2-byte little-endian identifier, a 1-byte offset,
	 * a 1-byte daylight saving time (DST) offset, and a 1-byte DST rules.
	 */
	val bytes: ByteArray
		get() = byteArrayOf(
			*identifierBytes,
			offsetByte,
			dstOffsetByte,
			dstRules,
		)

	/**
	 * Converts the city name to a byte array of 18 bytes.
	 * Each character of the city name is encoded, and the result is filled with zeros if the name
	 * is shorter than 18 bytes.
	 *
	 * @throws IllegalArgumentException if the city name cannot be encoded to a byte array
	 * properly.
	 */
	val cityNameBytes: ByteArray
		get() = cityName.encodeToByteArray().copyOf(18)

	/**
	 * The [cityCode] as 18 bytes, zero padded like [cityNameBytes], all zeros if there is none.
	 */
	val cityCodeBytes: ByteArray
		get() = (cityCode ?: "").encodeToByteArray().copyOf(18)

	/**
	 * Converts the offset, which is represented as a double indicating the time zone offset in hours, into a single byte.
	 * The conversion scales the offset by a factor of 4 because the offset is stored in 15-minute intervals.
	 * This means every unit increase in the byte value represents a 15-minute difference in the time zone offset.
	 * Thus, an offset of 1.0 (hour) is converted to a byte value of 4, representing a total of 4 * 15 minutes = 60 minutes.
	 * This byte can then be used to form part of the 5-byte array describing the time zone.
	 */
	val offsetByte: Byte
		get() = (offset * 4).toInt().toByte()

	/**
	 * Converts the daylight saving time difference (dstDiff) from hours into a single byte for compact storage.
	 * The dstDiff is multiplied by 4 to fit it into 15-minute intervals, similar to offset conversion.
	 * This scaled value provides a means to represent the DST offset adjustment within the time zone structure.
	 * It enables precise calculation of DST time changes, packaged for streamlined data handling in the byte array.
	 */
	val dstOffsetByte: Byte
		get() = (dstDiff * 4).toInt().toByte()

	/**
	 * Retrieves the multiband 6 radio ID associated with the specified timezone offset.
	 *
	 * @return The corresponding integer radio ID if found; otherwise, returns 0 if no matching timezone ID exists.
	 */
	val radioId: Int
		get() = timezoneRadioIdData.entries.firstOrNull {
			it.key.toDouble() == offset
		}?.value ?: 0

	val radioIdByte: Byte
		get() = radioId.toByte()

	abstract val coordinatesBytes: ByteArray

	/**
	 * Returns this timezone's effective UTC offset at [instant], including daylight saving time,
	 * in signed hours and minutes such as `+8:00` or `-12:45`.
	 */
	fun ianaOffsetString(instant: Instant = KotlinClock.System.now()): String {
		val totalMinutes = KotlinTimeZone.of(timeZone).offsetAt(instant).totalSeconds / 60
		val sign = if (totalMinutes < 0) "-" else "+"
		val absoluteMinutes = abs(totalMinutes)
		val hours = absoluteMinutes / 60
		val minutes = absoluteMinutes % 60

		return "$sign$hours:${minutes.toString().padStart(2, '0')}"
	}

	/**
	 * Returns whether the effective IANA offset at [instant] differs from this lookup entry's
	 * default [offset].
	 */
	fun isExpectedDst(instant: Instant = KotlinClock.System.now()): Boolean {
		val currentOffsetSeconds = KotlinTimeZone.of(timeZone).offsetAt(instant).totalSeconds
		val defaultOffsetSeconds = (offset * 60 * 60).roundToInt()

		return currentOffsetSeconds != defaultOffsetSeconds
	}
}
