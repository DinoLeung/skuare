package xyz.d1n0.model

import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.model.HomeTimeZone.Companion.homeTimeZones
import kotlinx.datetime.TimeZone as KotlinTimeZone
import kotlinx.datetime.Clock as KotlinClock
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime


data class HomeClock(
    override val timeZone: HomeTimeZone,
    override val dstStatus: DstStatus,
) : Clock(timeZone, dstStatus) {
    companion object {
        fun fromTimeZoneId(timeZoneId: Int, dstStatus: DstStatus) =
            homeTimeZones[timeZoneId]?.let {
                HomeClock(it, dstStatus)
            } ?: throw HomeClockError("Time Zone ID $timeZoneId not found")
    }

    fun getCurrentDateTime(): KotlinLocalDateTime {
        val timeZone = KotlinTimeZone.of(timeZone.timeZone)
        val now = KotlinClock.System.now()
        return now.toLocalDateTime(timeZone)
    }
}

class HomeClockError(message: String) : Exception(message)
