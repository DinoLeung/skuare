package xyz.d1n0.ui.text

import android.content.Context
import android.icu.text.DateFormat
import android.icu.util.TimeZone as IcuTimeZone
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toInstant
import android.text.format.DateFormat as AndroidDateFormat

/**
 * [LocaleFormatter] with ICU, in the app's locale (the per-app language if one is set) and the
 * user's 12 or 24 hour setting.
 */
class AndroidLocaleFormatter(private val context: Context) : LocaleFormatter {
	private val locale: Locale
		get() = context.resources.configuration.locales[0]

	override fun format(dateTime: LocalDateTime, skeleton: String): String {
		// ICU's j follows the locale, the user may have picked the other clock in the settings
		val hour = if (AndroidDateFormat.is24HourFormat(context)) "H" else "h"
		val formatter = DateFormat.getInstanceForSkeleton(skeleton.replace("j", hour), locale)
		// dateTime is formatted as if it were UTC, so no time zone or DST gap can shift it
		formatter.timeZone = IcuTimeZone.GMT_ZONE
		return formatter.format(Date(dateTime.toInstant(TimeZone.UTC).toEpochMilliseconds()))
	}

	override fun shortWeekdayName(day: DayOfWeek): String =
		day.toJavaDayOfWeek().getDisplayName(TextStyle.SHORT_STANDALONE, locale)
}
