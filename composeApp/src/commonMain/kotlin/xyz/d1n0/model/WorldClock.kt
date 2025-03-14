package xyz.d1n0.model

import kotlinx.serialization.Serializable
import xyz.d1n0.constant.DstStatus
import xyz.d1n0.constant.WorldTimeZoneData

@Serializable
data class WorldClock(
	override val timeZone: WorldTimeZone,
	override val dstStatus: DstStatus,
) : Clock() {
	companion object {
		fun fromTimeZoneId(timeZoneId: Int, dstStatus: DstStatus): WorldClock {
			require(WorldTimeZoneData.containsKey(timeZoneId)) { "Time Zone ID $timeZoneId not found" }
			return WorldClock(WorldTimeZoneData.getValue(timeZoneId), dstStatus)
		}
	}
}
