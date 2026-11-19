package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
