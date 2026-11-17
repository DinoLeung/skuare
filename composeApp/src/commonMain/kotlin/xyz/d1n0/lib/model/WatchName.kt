package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.helper.requireOpCode

data class WatchName(val value: String) {
	init {
		require(value.length <= 19) { "Watch name must not exceed 19 characters" }
	}

	companion object {
		fun fromPacket(packet: ByteArray): WatchName {
			packet.requireOpCode(OpCode.WATCH_NAME, "Watch name")
			require(packet.size == 20) {
				"Watch name packet must be exactly 20 bytes long, e.g. 23 43 41 53 49 4F 20 47 57 2D 42 35 36 30 30 23 00 00 00 00"
			}
			return WatchName(
				packet.drop(1).takeWhile { it != 0x00.toByte() }.toByteArray()
					.decodeToString()
			)
		}
	}

	val packet: ByteArray
		get() = ByteArray(20) { index ->
			when (index) {
				0 -> OpCode.WATCH_NAME.byte
				in 1..value.length -> value[index - 1].code.toByte()
				else -> 0x00.toByte()
			}
		}
}