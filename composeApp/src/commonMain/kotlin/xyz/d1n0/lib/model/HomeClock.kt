package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.HomeTimezoneData

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
}
