package xyz.d1n0.lib.constant

import kotlin.jvm.JvmInline

@JvmInline
value class AutoSyncDelay(val minutes: Int) {
	init {
		require(minutes in 0..59) {
			"AutoSyncDelay must be between 0 and 59, got $minutes"
		}
	}

	val byte: Byte
		get() = minutes.toByte()

	companion object {
		fun fromByte(byte: Byte): AutoSyncDelay =
			AutoSyncDelay(byte.toInt())
	}

	override fun toString(): String {
		return minutes.toString()
	}
}
