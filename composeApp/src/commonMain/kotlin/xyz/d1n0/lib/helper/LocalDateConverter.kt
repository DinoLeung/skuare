package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalDate

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

fun LocalDate.toMillis() = this.toEpochDays() * MILLIS_PER_DAY

fun LocalDate.Companion.fromMillis(millis: Long): LocalDate {
	val truncatedDays = millis / MILLIS_PER_DAY
	val epochDays = if (millis < 0 && millis % MILLIS_PER_DAY != 0L) truncatedDays - 1 else truncatedDays
	return fromEpochDays(epochDays.toInt())
}
