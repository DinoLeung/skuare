package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.Month.*
import kotlinx.datetime.number

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

fun LocalTime.toHourMinuteString() =
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