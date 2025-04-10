package xyz.d1n0.lib.constant

enum class DateFormat(val byte: Byte) {
    MDD(0),
    DDM(1);

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun fromByte(byte: Byte) =
            DateFormat.entries.firstOrNull { it.byte == byte }
                ?: error("Unknown date format value: ${byte.toHexString(HexFormat.UpperCase)}")
    }
}