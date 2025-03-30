package xyz.d1n0.constant

enum class WeekdayLanguage(val byte: Byte) {
    EN(0),
    ES(1),
    FR(2),
    DE(3),
    IT(4),
    RU(5);

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun fromByte(byte: Byte) =
            WeekdayLanguage.entries.firstOrNull { it.byte == byte }
                ?: error("Unknown language value: ${byte.toHexString(HexFormat.UpperCase)}")
    }
}