package xyz.d1n0.model

import xyz.d1n0.constant.DstStatus
import xyz.d1n0.constant.WorldTimeZoneData

data class WorldClock(
	override val timeZone: WorldTimeZone,
	override val dstStatus: DstStatus,
) : Clock(timeZone, dstStatus) {
	companion object {
		fun fromTimeZoneId(timeZoneId: Int, dstStatus: DstStatus): WorldClock {
			require(WorldTimeZoneData.containsKey(timeZoneId)) {
				"Time Zone ID $timeZoneId not found"
			}
			return WorldClock(WorldTimeZoneData[timeZoneId]!!, dstStatus)
		}
	}
}
