package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
	require(this <= 99.hours + 99.minutes + 99.seconds) { "Duration must not exceed 99:99:99" }
	val totalSeconds = this.inWholeSeconds

	val hours = (totalSeconds / 3600).toInt().coerceAtMost(99)
	val minutes = ((totalSeconds - hours * 3600) / 60).toInt().coerceAtMost(99)
	val seconds = (totalSeconds - hours * 3600 - minutes * 60).toInt().coerceAtMost(99)

	return hours.toString().padStart(2, '0') + minutes.toString()
		.padStart(2, '0') + seconds.toString().padStart(2, '0')
}

fun Duration.Companion.fromHHMMSS(text: String): Duration {
	require(text.length == 6) { "HHMMSS text must be exactly 6 characters in length" }
	val hours = text.slice(0..1).toInt().hours
	val minutes = text.slice(2..3).toInt().minutes
	val seconds = text.slice(4..5).toInt().seconds
	val duration = hours + minutes + seconds
	require(duration <= 99.hours + 99.minutes + 99.seconds) { "Duration must not exceed 99:99:99" }
	return duration
}

fun LocalTime.toHHMMString() =
	this.hour.toString().padStart(2, '0') + ":" + this.minute.toString().padStart(2, '0')

fun Month.abbreviatedName(): String = when (this) {
	Month.JANUARY -> "Jan"
	Month.FEBRUARY -> "Feb"
	Month.MARCH -> "Mar"
	Month.APRIL -> "Apr"
	Month.MAY -> "May"
	Month.JUNE -> "Jun"
	Month.JULY -> "Jul"
	Month.AUGUST -> "Aug"
	Month.SEPTEMBER -> "Sep"
	Month.OCTOBER -> "Oct"
	Month.NOVEMBER -> "Nov"
	Month.DECEMBER -> "Dec"
}

fun Month.fullName(): String = when (this) {
	Month.JANUARY -> "January"
	Month.FEBRUARY -> "February"
	Month.MARCH -> "March"
	Month.APRIL -> "April"
	Month.MAY -> "May"
	Month.JUNE -> "June"
	Month.JULY -> "July"
	Month.AUGUST -> "August"
	Month.SEPTEMBER -> "September"
	Month.OCTOBER -> "October"
	Month.NOVEMBER -> "November"
	Month.DECEMBER -> "December"
}

fun LocalDate.toMonthYear() = "${this.month.fullName()} ${this.year}"
fun LocalDate.toDayMonth() = "${this.dayOfMonth} ${this.month.fullName()}"
fun LocalDate.toDayMonthYear() = "${this.dayOfMonth} ${this.month.abbreviatedName()} ${this.year}"
