package xyz.d1n0.lib.constant

enum class ConnectionTimeout(val byte: Byte) {
	MINUTES_3(3),
	MINUTES_5(5),
	MINUTES_10(10);

	companion object {
		@OptIn(ExperimentalStdlibApi::class)
		fun fromByte(byte: Byte) =
			ConnectionTimeout.entries.firstOrNull { it.byte == byte }
				?: error("Unknown connection timeout value: ${byte.toHexString(HexFormat.UpperCase)}")
	}
}