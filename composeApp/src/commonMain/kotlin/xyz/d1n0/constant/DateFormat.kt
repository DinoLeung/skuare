package xyz.d1n0.constant

enum class DateFormat(val value: Int) {
    MDD(0),
    DDM(1);

    companion object {
        fun fromValue(value: Int) =
            DateFormat.entries.firstOrNull { it.value == value } ?: error("Unknown date format value: $value")
    }
}