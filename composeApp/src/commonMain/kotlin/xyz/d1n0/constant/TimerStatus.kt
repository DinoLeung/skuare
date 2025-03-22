package xyz.d1n0.constant

enum class TimerStatus(val value: Int) {
    NOT_STARTED(0),
    ACTIVE(1),
    SUSPENDED(2);

    companion object {
        fun fromValue(value: Int) =
            TimerStatus.entries.firstOrNull { it.value == value } ?: error("Unknown timer status value: $value")
    }
}
