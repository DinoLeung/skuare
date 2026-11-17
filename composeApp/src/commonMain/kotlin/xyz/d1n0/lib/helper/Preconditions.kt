package xyz.d1n0.lib.helper

import xyz.d1n0.lib.constant.OpCode

inline fun Int.requireIn(range: IntRange, lazyMsg: () -> Any): Int =
	if (this in range) this else throw IllegalArgumentException(lazyMsg().toString())

/**
 * Checks that this packet starts with [opCode].
 *
 * @param name What the packet is, for the error message, e.g. "Alarm A".
 * @throws IllegalArgumentException if it starts with another byte or is empty.
 */
fun ByteArray.requireOpCode(opCode: OpCode, name: String) {
	require(firstOrNull() == opCode.byte) {
		"$name packet must start with command code ${opCode.byte.toHexString(HexFormat.UpperCase)}"
	}
}
