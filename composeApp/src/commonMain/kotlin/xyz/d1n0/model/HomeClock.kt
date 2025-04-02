package xyz.d1n0.model

import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.constant.OpCode
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
        fun fromTimeZoneId(timeZoneId: Short, dstSettings: DstSettings): HomeClock {
            require(HomeTimezoneData.containsKey(timeZoneId.toInt())) { "Time Zone ID $timeZoneId not found" }
            return HomeClock(HomeTimezoneData.getValue(timeZoneId.toInt()), dstSettings)
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
    fun getCurrentDateTimePacket(delay: Duration = 0.seconds) =
        byteArrayOf(
            OpCode.CURRENT_TIME.byte,
            *getCurrentDateTime(delay).toByteArray(),
            0x01.toByte()
        )
}
