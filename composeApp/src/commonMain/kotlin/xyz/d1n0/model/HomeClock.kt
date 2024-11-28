package xyz.d1n0.model

import kotlinx.datetime.Instant


data class HomeClock(
    override val timeZone: HomeTimeZone,
    override val dstStatus: DstStatus,
) : Clock(timeZone, dstStatus) {
    companion object {
        fun fromTimeZoneId(timeZoneId: String, dstStatus: DstStatus) = HomeClock(HomeTimeZone.fuzzySearch(timeZoneId).first(), dstStatus)
    }

    suspend fun getCurrentDateTime() : Instant {
        TODO("implement me, figure out whcih object is best suit")
    }
}