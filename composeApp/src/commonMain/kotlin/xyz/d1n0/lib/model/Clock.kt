package xyz.d1n0.lib.model

abstract class Clock {
	abstract val timeZone: Timezone
	abstract val dstSettings: DstSettings
}
