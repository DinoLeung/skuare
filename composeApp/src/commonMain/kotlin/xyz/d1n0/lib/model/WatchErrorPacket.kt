package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode

/**
 * An error the watch sent instead of a reply, e.g. FF 81 1D for a clocks request past the last
 * clock the watch has.
 *
 * Known layout (from FF 81 1D):
 * [0] [OpCode.ERROR]
 * [1] unknown
 * [2] opcode of the request that failed, probably
 */
@OptIn(ExperimentalStdlibApi::class)
class WatchErrorPacket(val packet: ByteArray) :
	Exception("Watch replied with error ${packet.toHexString(HexFormat.UpperCase)}") {

	init {
		require(packet.firstOrNull() == OpCode.ERROR.byte) {
			"Error packet must start with ${OpCode.ERROR.byte.toHexString(HexFormat.UpperCase)}"
		}
	}

	/** The packet as spaced hex, e.g. FF 81 1D. */
	val hex: String
		get() = packet.joinToString(" ") { it.toHexString(HexFormat.UpperCase) }

	/** Opcode of the request that failed, null if the packet has none or it is unknown. */
	val failedOpCode: OpCode?
		get() = packet.getOrNull(2)?.let(OpCode::fromByte)
}
