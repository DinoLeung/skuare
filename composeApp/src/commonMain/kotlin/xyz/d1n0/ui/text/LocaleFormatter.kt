package xyz.d1n0.ui.text

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Formats dates in the user's language and region, implemented per platform with its own
 * locale data, so no month or weekday names need translating.
 */
interface LocaleFormatter {
	/**
	 * Formats [dateTime] with a CLDR skeleton, the fields to show without their order or
	 * punctuation, which the locale decides. E.g. "yMMMd" is 8 Oct 2026 in en-GB and Oct 8, 2026
	 * in en-US. "j" is the hour on the user's 12 or 24 hour clock.
	 */
	fun format(dateTime: LocalDateTime, skeleton: String): String

	/** Abbreviated name of [day] on its own, e.g. Mon. */
	fun shortWeekdayName(day: DayOfWeek): String
}

/**
 * The [LocaleFormatter] for the UI, [xyz.d1n0.App] provides the platform's from Koin. Previews get
 * [IsoLocaleFormatter].
 */
val LocalLocaleFormatter = staticCompositionLocalOf<LocaleFormatter> { IsoLocaleFormatter }

/** ISO dates and English weekdays regardless of the locale, for previews and tests. */
object IsoLocaleFormatter : LocaleFormatter {
	override fun format(dateTime: LocalDateTime, skeleton: String): String =
		if (skeleton.any { it in "jHhm" }) "${dateTime.date} ${dateTime.time}" else dateTime.date.toString()

	override fun shortWeekdayName(day: DayOfWeek): String =
		day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
}

/** e.g. 8 Oct 26, short enough for two date fields side by side. */
fun LocaleFormatter.formatShortDate(date: LocalDate): String = format(date.atTime(0, 0), "yyMMMd")

/** e.g. Thursday, 8 October 2026, for screen readers. */
fun LocaleFormatter.formatFullDate(date: LocalDate): String = format(date.atTime(0, 0), "yMMMMEEEEd")

/** e.g. October 2026, for a calendar's month. */
fun LocaleFormatter.formatMonthYear(date: LocalDate): String = format(date.atTime(0, 0), "yMMMM")

/** e.g. 8 Oct 2026, 14:05, in the device's time zone. */
fun LocaleFormatter.formatDateTime(instant: Instant): String =
	format(instant.toLocalDateTime(TimeZone.currentSystemDefault()), "yMMMdjmm")
