package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalTime

fun Double.toSignedString(): String = when {
    this == 0.0 -> "±${this.toString()}"
    this > 0.0 -> "+${this.toString()}"
    else -> this.toString()
}

fun LocalTime.toHourMinuteString() =
    this.hour.toString().padStart(2, '0') + ":" + this.minute.toString().padStart(2, '0')