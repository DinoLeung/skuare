package xyz.d1n0.model

import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.constant.DstStatus
import xyz.d1n0.constant.HomeTimeZoneData
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
	 * Returns the current local date and time for the HomeClock's time zone.
	 *
	 * Utilizes the Kotlin system clock to obtain the current time and converts it to local date and time
	 * based on the `HomeClock` instance's assigned time zone.
	 *
	 * @return A `KotlinLocalDateTime` object representing the current date and time in the specified time zone.
	 */
	fun getCurrentDateTime(): KotlinLocalDateTime {
		val timeZone = KotlinTimeZone.of(timeZone.timeZone)
		val now = KotlinClock.System.now()
		return now.toLocalDateTime(timeZone)
	}
}

class HomeClockError(message: String) : Exception(message)
