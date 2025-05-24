package xyz.d1n0.ui.boilerplate

import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.lib.helper.toDayMonthYear
import xyz.d1n0.lib.helper.toMonthYear
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
class DatePickerCustomerFormatter : DatePickerFormatter {
	@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
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

	@OptIn(ExperimentalMaterial3Api::class)
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
