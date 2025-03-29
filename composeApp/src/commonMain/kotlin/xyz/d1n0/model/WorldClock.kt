package xyz.d1n0.model

import xyz.d1n0.constant.WorldTimezoneData

data class WorldClock(
	override val timeZone: WorldTimezone,
	override val dstSettings: DstSettings,
) : Clock() {
	companion object {
		fun fromTimeZoneId(timeZoneId: Int, dstSettings: DstSettings): WorldClock {
			require(WorldTimezoneData.containsKey(timeZoneId)) { "Time Zone ID $timeZoneId not found" }
			return WorldClock(WorldTimezoneData.getValue(timeZoneId), dstSettings)
		}
	}
}
