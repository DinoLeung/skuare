package xyz.d1n0.ui.boilerplate

import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.datetime.LocalDate
import xyz.d1n0.lib.helper.fromMillis
import xyz.d1n0.lib.helper.toDayMonthYear
import xyz.d1n0.lib.helper.toMonthYear

/**
 * The picker's millis are UTC midnights, read as UTC so the date can't shift a day in the device's
 * time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
class DatePickerCustomFormatter : DatePickerFormatter {
	override fun formatMonthYear(
		monthMillis: Long?,
		locale: CalendarLocale,
	): String? {
		if (monthMillis == null) return null
		return LocalDate.fromMillis(monthMillis).toMonthYear()
	}

	override fun formatDate(
		dateMillis: Long?,
		locale: CalendarLocale,
		forContentDescription: Boolean,
	): String? {
		if (dateMillis == null) return null
		return LocalDate.fromMillis(dateMillis).toDayMonthYear()
	}
}
