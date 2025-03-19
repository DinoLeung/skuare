package xyz.d1n0.constant

enum class AutoSyncEnabled(val value: Int) {
	ENABLED(0x00),
	DISABLED(0x80);

	companion object {
		fun fromValue(value: Int) =
			AutoSyncEnabled.entries.firstOrNull { it.value == value } ?: error("Unknown backlight duration value: $value")
	}
}