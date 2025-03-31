package xyz.d1n0.model

abstract class Clock {
	abstract val timeZone: Timezone
	abstract val dstSettings: DstSettings
}
