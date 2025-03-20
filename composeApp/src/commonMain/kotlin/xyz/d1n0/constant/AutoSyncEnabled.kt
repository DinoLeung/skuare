package xyz.d1n0.constant

enum class AutoSyncEnabled(val value: Int) {
	ENABLED(0x00),
	DISABLED(0x80);

	@OptIn(ExperimentalStdlibApi::class)
	companion object {
		fun fromValue(value: Int) =
			AutoSyncEnabled.entries.firstOrNull { it.value == value } ?: error("Unknown auto sync enabled value: ${value.toHexString(HexFormat.UpperCase)}")
	}
}