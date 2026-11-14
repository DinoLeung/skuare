package xyz.d1n0.lib.constant

/**
 * Battery level byte of the watch status, [bars] as shown in the official app.
 *
 * Not the whole scale yet, see research/battery-research.md, so unknown bytes give null instead of
 * an error.
 */
enum class BatteryLevel(val byte: Byte, val bars: Int) {
	LEVEL_1(0x10, 1), // Not observed yet, extrapolated
	LEVEL_2(0x11, 2),
	LEVEL_3(0x12, 3),
	LEVEL_4(0x13, 4),
	LEVEL_5(0x14, 5); // Not observed yet, extrapolated

	companion object {
		/** The level for [byte], null if it isn't known yet. */
		fun fromByte(byte: Byte): BatteryLevel? = BatteryLevel.entries.firstOrNull { it.byte == byte }
	}
}
