package xyz.d1n0.lib.constant

enum class ConnectReason(val byte: Byte) {
	SETUP(0),       // Lower left button long, need to set the auto sync offset
	DEFAULT(1),     // Lower left button long
	FIND(2),        // Lower right button long
	AUTO_SYNC(3),   // Periodic connect at 6:xx, 12:xx, 18:xx, 00:xx
	MANUAL_SYNC(4); // Lower right button short

	companion object {
		@OptIn(ExperimentalStdlibApi::class)
		fun fromByte(byte: Byte) =
			ConnectReason.entries.firstOrNull { it.byte == byte }
				?: error("Unknown connect reason value: ${byte.toHexString(HexFormat.UpperCase)}")
	}
}