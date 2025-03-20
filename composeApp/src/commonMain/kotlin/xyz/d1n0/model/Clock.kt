package xyz.d1n0.model

abstract class Clock {
	abstract open val timeZone: TimeZone
	abstract open val dstSettings: DstSettings
}
