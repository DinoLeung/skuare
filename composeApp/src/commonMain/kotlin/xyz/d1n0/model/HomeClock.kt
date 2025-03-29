package xyz.d1n0.model

import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.constant.Command
import xyz.d1n0.constant.HomeTimezoneData
import xyz.d1n0.helper.toByteArray
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock as KotlinClock
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import kotlinx.datetime.TimeZone as KotlinTimeZone

data class HomeClock(
    override val timeZone: HomeTimezone,
    override val dstSettings: DstSettings,
) : Clock() {
    companion object {
        /**
         * Creates a `HomeClock` object from a given time zone ID and daylight saving time (DST) status.
         *
         * This method searches the predefined `homeTimeZones` map for the specified time zone ID and returns a
         * `HomeClock` object if found, using the provided DST status. If the time zone ID is not found, it throws
         * a `IllegalArgumentException `.
         *
         * @param timeZoneId The unique identifier for the target time zone in the `homeTimeZones` collection.
         * @param dstStatus The daylight saving time status to be applied to the `HomeClock`.
         * @return A `HomeClock` instance configured with the found time zone and the provided DST status.
         * @throws IllegalArgumentException  If the specified time zone ID does not exist in `homeTimeZones`.
         */
        fun fromTimeZoneId(timeZoneId: Int, dstSettings: DstSettings): HomeClock {
            require(HomeTimezoneData.containsKey(timeZoneId)) { "Time Zone ID $timeZoneId not found" }
            return HomeClock(HomeTimezoneData.getValue(timeZoneId), dstSettings)
        }
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
     * @param delay An optional `Duration` to adjust the current time by adding a specified delay.
     * @return A `KotlinLocalDateTime` object representing the adjusted current date and time.
     */
    fun getCurrentDateTime(delay: Duration?): KotlinLocalDateTime {
        val timeZone = KotlinTimeZone.of(timeZone.timeZone)
        val now = KotlinClock.System.now() + (delay ?: 0.seconds)
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
     * @param delay An optional `Duration` to adjust the current time by adding a specified delay.
     * @return A `ByteArray` containing the command identifier, the byte representation of the adjusted current date and time, and an additional status byte.
     */
    fun getCurrentDateTimePacket(delay: Duration?) =
        byteArrayOf(
            Command.CURRENT_TIME.value.toByte(),
            *getCurrentDateTime(delay).toByteArray(),
            0x01.toByte()
        )
}
