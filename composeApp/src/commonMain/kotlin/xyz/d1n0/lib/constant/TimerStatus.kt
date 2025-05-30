package xyz.d1n0.lib.constant

enum class TimerStatus(val byte: Byte) {
	NOT_STARTED(0), ACTIVE(1), SUSPENDED(2);

	companion object {
		@OptIn(ExperimentalStdlibApi::class)
		fun fromByte(byte: Byte) = TimerStatus.entries.firstOrNull { it.byte == byte }
			?: error("Unknown timer status value: ${byte.toHexString(HexFormat.UpperCase)}")
	}

	override fun toString(): String = when (this) {
		NOT_STARTED -> "Not Started"
		ACTIVE -> "Active"
		SUSPENDED -> "Suspended"
	}
}
