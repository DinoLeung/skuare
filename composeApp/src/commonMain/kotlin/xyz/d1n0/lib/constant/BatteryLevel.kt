package xyz.d1n0.lib.constant

/**
 * Battery level byte of the watch status, [bars] as shown in the official app.
 */
enum class BatteryLevel(val byte: Byte, val bars: Int) {
	LEVEL_1(0x10, 1), // Not observed yet, extrapolated
	LEVEL_2(0x11, 2),
	LEVEL_3(0x12, 3),
	LEVEL_4(0x13, 4),
	LEVEL_5(0x14, 5); // Not observed yet, extrapolated

	companion object {
		@OptIn(ExperimentalStdlibApi::class)
		fun fromByte(byte: Byte) =
			BatteryLevel.entries.firstOrNull { it.byte == byte }
				?: error("Unknown battery level value: ${byte.toHexString(HexFormat.UpperCase)}")
	}
}
