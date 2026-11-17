package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.HomeTimezoneData

data class HomeClock(
	override val timeZone: HomeTimezone,
	override val dstSettings: DstSettings,
) : Clock() {
	companion object {
		/**
		 * @param timeZoneId Identifier of a time zone in [HomeTimezoneData].
		 * @param dstSettings The DST settings of the clock.
		 * @throws IllegalArgumentException if [timeZoneId] is not in [HomeTimezoneData].
		 */
		fun fromTimeZoneId(timeZoneId: Short, dstSettings: DstSettings): HomeClock {
			val timeZone = requireNotNull(HomeTimezoneData[timeZoneId.toInt()]) { "Time Zone ID $timeZoneId not found" }
			return HomeClock(timeZone, dstSettings)
		}
	}
}
