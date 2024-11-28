package xyz.d1n0.model

sealed class Clock (
    open val timeZone: TimeZone,
    open val dstStatus: DstStatus,
)
