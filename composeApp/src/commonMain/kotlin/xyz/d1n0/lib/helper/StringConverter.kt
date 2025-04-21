package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.Month.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

fun Int.toOrdinalString(): String = when {
        this % 100 in 11..13 -> "${this}th"
        this % 10 == 1 -> "${this}st"
        this % 10 == 2 -> "${this}nd"
        this % 10 == 3 -> "${this}rd"
        else -> "${this}th"
    }

fun Double.toSignedString(): String = when {
    this == 0.0 -> "±${this.toString()}"
    this > 0.0 -> "+${this.toString()}"
    else -> this.toString()
}

fun Duration.toHHMMSSString(): String {
    require (this < 1.days) { "Duration must not exceed 24 hours" }
    val hours = this.inWholeHours.toString().padStart(2, '0')
    val minutes = (this.inWholeMinutes - (this.inWholeHours * 60)).toString().padStart(2, '0')
    val seconds = (this.inWholeSeconds - (this.inWholeMinutes * 60)).toString().padStart(2, '0')
    return "$hours$minutes$seconds"
}

fun Duration.Companion.fromHHMMSS(text: String): Duration {
    require (text.length == 6) { "HHMMSS text must be exactly 6 characters in length" }
    val hours = text.slice(0..1).toInt().hours
    val minutes = text.slice(2..3).toInt().minutes
    val seconds = text.slice(4..5).toInt().seconds
    val duration = hours + minutes + seconds
    require (duration < 1.days) { "Duration must not exceed 24 hours" }
    return duration
}

fun LocalTime.toHHMMString() =
    this.hour.toString().padStart(2, '0') + ":" + this.minute.toString().padStart(2, '0')

fun Month.abbreviatedName(): String = when (this) {
    JANUARY -> "Jan"
    FEBRUARY -> "Feb"
    MARCH -> "Mar"
    APRIL -> "Apr"
    MAY -> "May"
    JUNE -> "Jun"
    JULY -> "Jul"
    AUGUST -> "Aug"
    SEPTEMBER -> "Sep"
    OCTOBER -> "Oct"
    NOVEMBER -> "Nov"
    DECEMBER -> "Dec"
    else -> error("Unknown month: $this")
}

fun LocalDate.toDayMonth() = "${this.dayOfMonth} ${this.month.abbreviatedName()}"
fun LocalDate.toDayMonthYear() = "${this.dayOfMonth} ${this.month.abbreviatedName()} ${this.year}"