package xyz.d1n0.model

import xyz.d1n0.constant.WorldTimeZoneData

data class WorldClock(
	override val timeZone: WorldTimeZone,
	override val dstSettings: DstSettings,
) : Clock() {
	companion object {
		fun fromTimeZoneId(timeZoneId: Int, dstSettings: DstSettings): WorldClock {
			require(WorldTimeZoneData.containsKey(timeZoneId)) { "Time Zone ID $timeZoneId not found" }
			return WorldClock(WorldTimeZoneData.getValue(timeZoneId), dstSettings)
		}
	}
}
