package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalDate

fun LocalDate.toMillis() = this.toEpochDays() * 24L * 60L * 60L * 1000L

fun LocalDate.Companion.fromMillis(millis: Long) =
    fromEpochDays((millis / (24L * 60L * 60L * 1000L)).toInt())