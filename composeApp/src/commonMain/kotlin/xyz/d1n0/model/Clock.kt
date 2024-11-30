package xyz.d1n0.model

import xyz.d1n0.constant.DstStatus

sealed class Clock(
    open val timeZone: TimeZone,
    open val dstStatus: DstStatus,
)
