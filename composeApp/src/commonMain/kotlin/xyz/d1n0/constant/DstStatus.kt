package xyz.d1n0.constant

enum class DstStatus(val value: Int) {
	// 00000000
	MANUAL_OFF(0),

	// 00000001
	MANUAL_ON(1),

	// 00000010
	AUTO_OFF(2),

	// 00000011
	AUTO_ON(3);

	companion object {
		fun fromValue(value: Int) =
			entries.firstOrNull { it.value == value } ?: error("Unknown DST status value: $value")
	}
}
