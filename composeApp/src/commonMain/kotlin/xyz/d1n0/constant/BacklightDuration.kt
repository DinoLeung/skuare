package xyz.d1n0.constant

enum class BacklightDuration(val value: Int) {
    SHORT(0),
    LONG(1);

    companion object {
        fun fromValue(value: Int) =
            BacklightDuration.entries.firstOrNull { it.value == value } ?: error("Unknown backlight duration value: $value")
    }
}