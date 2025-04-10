package xyz.d1n0.lib.constant

enum class BacklightDuration(val byte: Byte) {
    SHORT(0),
    LONG(1);

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun fromByte(byte: Byte) =
            BacklightDuration.entries.firstOrNull { it.byte == byte }
                ?: error("Unknown backlight duration value: ${byte.toHexString(HexFormat.UpperCase)}")
    }
}