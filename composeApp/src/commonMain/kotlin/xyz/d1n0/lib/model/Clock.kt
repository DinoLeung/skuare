package xyz.d1n0.lib.model

import xyz.d1n0.lib.helper.toSignedString

abstract class Clock {
	abstract val timeZone: Timezone
	abstract val dstSettings: DstSettings

	fun offsetString(): String {
		val finalOffset =
			if (dstSettings.enable) timeZone.offset + timeZone.dstDiff else timeZone.offset
		return finalOffset.toSignedString()
	}
}
