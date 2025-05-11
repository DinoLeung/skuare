package xyz.d1n0.lib.constant

enum class DateFormat(val byte: Byte) {
	MMDD(0),
	DDMM(1);

	companion object {
		@OptIn(ExperimentalStdlibApi::class)
		fun fromByte(byte: Byte) =
			DateFormat.entries.firstOrNull { it.byte == byte }
				?: error("Unknown date format value: ${byte.toHexString(HexFormat.UpperCase)}")
	}

	override fun toString(): String = when (this) {
		MMDD -> "MM.DD"
		DDMM -> "DD.MM"
	}
}