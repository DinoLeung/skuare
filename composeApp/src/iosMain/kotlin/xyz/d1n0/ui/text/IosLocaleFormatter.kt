package xyz.d1n0.ui.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toNSDate
// star import: currentLocale and timeZoneForSecondsFromGMT come from Objective-C categories
import platform.Foundation.*

/**
 * [LocaleFormatter] with NSDateFormatter, in the current locale. Its templates follow the user's
 * 12 or 24 hour setting for "j" themselves.
 */
class IosLocaleFormatter : LocaleFormatter {
	override fun format(dateTime: LocalDateTime, skeleton: String): String {
		val formatter = NSDateFormatter()
		formatter.locale = NSLocale.currentLocale
		// Gregorian like the date picker's grid and the watch, whatever the locale's own calendar
		formatter.calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
		// dateTime is formatted as if it were UTC, so no time zone or DST gap can shift it
		formatter.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
		formatter.setLocalizedDateFormatFromTemplate(skeleton)
		return formatter.stringFromDate(dateTime.toInstant(TimeZone.UTC).toNSDate())
	}

	override fun shortWeekdayName(day: DayOfWeek): String {
		val formatter = NSDateFormatter()
		formatter.locale = NSLocale.currentLocale
		// the symbols start on Sunday
		return formatter.shortStandaloneWeekdaySymbols[day.isoDayNumber % 7] as String
	}
}
