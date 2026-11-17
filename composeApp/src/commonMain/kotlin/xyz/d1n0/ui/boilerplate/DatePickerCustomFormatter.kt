package xyz.d1n0.ui.boilerplate

import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.lib.helper.toDayMonthYear
import xyz.d1n0.lib.helper.toMonthYear
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
class DatePickerCustomFormatter : DatePickerFormatter {
	override fun formatMonthYear(
		monthMillis: Long?,
		locale: CalendarLocale,
	): String? {
		if (monthMillis == null) return null
		return Instant.fromEpochMilliseconds(monthMillis)
			.toLocalDateTime(TimeZone.currentSystemDefault())
			.date
			.toMonthYear()
	}

	override fun formatDate(
		dateMillis: Long?,
		locale: CalendarLocale,
		forContentDescription: Boolean,
	): String? {
		if (dateMillis == null) return null
		return Instant.fromEpochMilliseconds(dateMillis)
			.toLocalDateTime(TimeZone.currentSystemDefault())
			.date
			.toDayMonthYear()
	}
}
