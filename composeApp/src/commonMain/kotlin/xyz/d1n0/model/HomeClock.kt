package xyz.d1n0.model

import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.constant.Command
import xyz.d1n0.constant.DstStatus
import xyz.d1n0.constant.HomeTimeZoneData
import xyz.d1n0.helper.BytesConverter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlinx.datetime.Clock as KotlinClock
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime


data class HomeClock(
	override val timeZone: HomeTimeZone,
	override val dstStatus: DstStatus,
) : Clock(timeZone, dstStatus) {
	companion object {
		/**
		 * Creates a `HomeClock` object from a given time zone ID and daylight saving time (DST) status.
		 *
		 * This method searches the predefined `homeTimeZones` map for the specified time zone ID and returns a
		 * `HomeClock` object if found, using the provided DST status. If the time zone ID is not found, it throws
		 * a `HomeClockError`.
		 *
		 * @param timeZoneId The unique identifier for the target time zone in the `homeTimeZones` collection.
		 * @param dstStatus The daylight saving time status to be applied to the `HomeClock`.
		 * @return A `HomeClock` instance configured with the found time zone and the provided DST status.
		 * @throws HomeClockError If the specified time zone ID does not exist in `homeTimeZones`.
		 */
		fun fromTimeZoneId(timeZoneId: Int, dstStatus: DstStatus) =
			HomeTimeZoneData.all[timeZoneId]?.let {
				HomeClock(it, dstStatus)
			} ?: throw HomeClockError("Time Zone ID $timeZoneId not found")
	}

	/**
	 * Represents the delay interval between transmission attempts or messages.
	 *
	 * `transmissionDelay` is used to define a fixed period that the system will wait between successive data
	 * transmissions. This delay is critical in ensuring that transmissions are not too frequent, which could
	 * lead to network congestion or data processing bottlenecks.
	 *
	 * The value is set to 300 milliseconds by default, but it can be adjusted according to specific requirements
	 * and network conditions to balance between responsiveness and resource utilization.
	 */


	/**
	 * Retrieves the current local date and time with an optional delay adjustment.
	 *
	 * This function calculates the current date and time based on the system clock, with an optional delay
	 * that can be added to the current time. The resulting date and time are converted to a local date and
	 * time format using a specified time zone.
	 *
	 * @param delay An optional `Duration` to adjust the current time by adding a specified delay. The defaulting to 300 milliseconds.
	 * @return A `KotlinLocalDateTime` object representing the adjusted current date and time.
	 */
	fun getCurrentDateTime(delay: Duration = 300.milliseconds): KotlinLocalDateTime {
		val timeZone = KotlinTimeZone.of(timeZone.timeZone)
		val now = KotlinClock.System.now() + delay
		return now.toLocalDateTime(timeZone)
	}

	/**
	 * Constructs a byte array representing the current date and time packet.
	 *
	 * This function creates a packet formatted for transmission that includes the current date and time
	 * information, as well as a command identifier byte and a status byte. The packet is constructed by
	 * converting the current date and time, adjusted by an optional delay, into a byte array and
	 * appending necessary metadata for transmission.
	 *
	 * @param delay An optional `Duration` to adjust the current time by adding a specified delay. The default is 300 milliseconds.
	 * @return A `ByteArray` containing the command identifier, the byte representation of the adjusted current date and time, and an additional status byte.
	 */
	fun getCurrentDateTimePacket(delay: Duration = 300.milliseconds) =
		byteArrayOf(
			Command.CURRENT_TIME.value.toByte(),
			*BytesConverter.dateTimeToByteArray(getCurrentDateTime(delay)),
			0x01.toByte()
		)
}

class HomeClockError(message: String) : Exception(message)
