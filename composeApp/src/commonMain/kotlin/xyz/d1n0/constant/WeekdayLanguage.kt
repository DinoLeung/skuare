package xyz.d1n0.constant

enum class WeekdayLanguage(val value: Int) {
    EN(0),
    ES(1),
    FR(2),
    DE(3),
    IT(4),
    RU(5);

    companion object {
        fun fromValue(value: Int) =
            WeekdayLanguage.entries.firstOrNull { it.value == value } ?: error("Unknown language value: $value")
    }
}