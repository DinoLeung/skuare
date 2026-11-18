package xyz.d1n0.ui.boilerplate

import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.datetime.LocalDate
import xyz.d1n0.lib.helper.fromMillis
import xyz.d1n0.ui.text.LocaleFormatter
import xyz.d1n0.ui.text.formatFullDate
import xyz.d1n0.ui.text.formatMonthYear
import xyz.d1n0.ui.text.formatShortDate

/**
 * Formats the date picker's dates like the date fields, with [formatter]. The picker's millis are
 * UTC midnights, read as UTC so the date can't shift a day in the device's time zone.
 * [CalendarLocale] is ignored, [formatter] already uses the app's locale.
 */
@OptIn(ExperimentalMaterial3Api::class)
class DatePickerCustomFormatter(private val formatter: LocaleFormatter) : DatePickerFormatter {
	override fun formatMonthYear(
		monthMillis: Long?,
		locale: CalendarLocale,
	): String? = monthMillis?.let { formatter.formatMonthYear(LocalDate.fromMillis(it)) }

	override fun formatDate(
		dateMillis: Long?,
		locale: CalendarLocale,
		forContentDescription: Boolean,
	): String? = dateMillis?.let {
		val date = LocalDate.fromMillis(it)
		if (forContentDescription) formatter.formatFullDate(date) else formatter.formatShortDate(date)
	}
}
