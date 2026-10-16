package xyz.d1n0.lib.constant

enum class ClocksUpdateSession(val byte: Byte) {
	START(0),
	END(1);

	companion object {
		@OptIn(ExperimentalStdlibApi::class)
		fun fromByte(byte: Byte) =
			ClocksUpdateSession.entries.firstOrNull { it.byte == byte }
				?: error("Unknown clocks update session value: ${byte.toHexString(HexFormat.UpperCase)}")
	}
}
