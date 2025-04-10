package xyz.d1n0.lib.model

import xyz.d1n0.constant.WorldTimezoneData

data class WorldClock(
	override val timeZone: WorldTimezone,
	override val dstSettings: DstSettings,
) : Clock() {
	companion object {
		fun fromTimeZoneId(timeZoneId: Short, dstSettings: DstSettings): WorldClock {
			require(WorldTimezoneData.containsKey(timeZoneId.toInt())) { "Time Zone ID $timeZoneId not found" }
			return WorldClock(WorldTimezoneData.getValue(timeZoneId.toInt()), dstSettings)
		}
	}
}
