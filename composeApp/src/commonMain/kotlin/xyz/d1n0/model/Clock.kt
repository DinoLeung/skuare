package xyz.d1n0.model

import xyz.d1n0.constant.DstStatus

abstract class Clock {
	abstract open val timeZone: TimeZone
	abstract open val dstStatus: DstStatus
}
