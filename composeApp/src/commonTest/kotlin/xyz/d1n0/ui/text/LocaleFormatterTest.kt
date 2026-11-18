package xyz.d1n0.ui.text

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleFormatterTest {
	/** Records what it was asked to format. */
	private class RecordingFormatter : LocaleFormatter {
		val calls = mutableListOf<Pair<LocalDateTime, String>>()

		override fun format(dateTime: LocalDateTime, skeleton: String): String {
			calls += dateTime to skeleton
			return ""
		}

		override fun shortWeekdayName(day: DayOfWeek): String = day.name
	}

	@Test
	fun dateHelpersFormatTheDateAtMidnightWithTheirSkeleton() {
		val formatter = RecordingFormatter()
		val date = LocalDate(2026, 10, 8)

		formatter.formatShortDate(date)
		formatter.formatFullDate(date)
		formatter.formatMonthYear(date)

		assertEquals(
			listOf(
				LocalDateTime(2026, 10, 8, 0, 0) to "yyMMMd",
				LocalDateTime(2026, 10, 8, 0, 0) to "yMMMMEEEEd",
				LocalDateTime(2026, 10, 8, 0, 0) to "yMMMM",
			),
			formatter.calls,
		)
	}

	@Test
	fun isoFormatterShowsTimeOnlyWhenTheSkeletonHasOne() {
		val dateTime = LocalDateTime(2026, 10, 8, 14, 5)
		assertEquals("2026-10-08", IsoLocaleFormatter.format(dateTime, "yyMMMd"))
		assertEquals("2026-10-08 14:05", IsoLocaleFormatter.format(dateTime, "yMMMdjmm"))
		assertEquals("Mon", IsoLocaleFormatter.shortWeekdayName(DayOfWeek.MONDAY))
	}
}
