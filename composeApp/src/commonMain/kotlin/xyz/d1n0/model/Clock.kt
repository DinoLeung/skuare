package xyz.d1n0.model

abstract class Clock {
	abstract open val timeZone: Timezone
	abstract open val dstSettings: DstSettings
}
