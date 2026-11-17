package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.WorldTimezoneData

data class WorldClock(
	override val timeZone: WorldTimezone,
	override val dstSettings: DstSettings,
) : Clock() {
	companion object {
		/**
		 * @param timeZoneId Identifier of a time zone in [WorldTimezoneData].
		 * @param dstSettings The DST settings of the clock.
		 * @throws IllegalArgumentException if [timeZoneId] is not in [WorldTimezoneData].
		 */
		fun fromTimeZoneId(timeZoneId: Short, dstSettings: DstSettings): WorldClock {
			val timeZone = requireNotNull(WorldTimezoneData[timeZoneId.toInt()]) { "Time Zone ID $timeZoneId not found" }
			return WorldClock(timeZone, dstSettings)
		}
	}
}
