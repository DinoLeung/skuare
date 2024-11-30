package xyz.d1n0.model

import xyz.d1n0.constant.DstStatus
import xyz.d1n0.constant.WorldTimeZoneData

data class WorldClock(
    override val timeZone: WorldTimeZone,
    override val dstStatus: DstStatus,
) : Clock(timeZone, dstStatus) {
    companion object {
        fun fromTimeZoneId(timeZoneId: Int, dstStatus: DstStatus) =
            WorldTimeZoneData.all[timeZoneId]?.let {
                WorldClock(it, dstStatus)
            } ?: throw WorldClockError("Time Zone ID $timeZoneId not found")
    }
}

class WorldClockError(message: String) : Exception(message)
