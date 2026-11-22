package xyz.d1n0.ui.screen.locations

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Instant

/**
 * How old a location must be for "Delete older than…". Months and years are calendar ones in the
 * phone's time zone, e.g. a month before 31 March is 28 or 29 February.
 */
enum class LocationAge(private val amount: Int, private val unit: DateTimeUnit.DateBased) {
	WEEK(1, DateTimeUnit.WEEK),
	MONTH(1, DateTimeUnit.MONTH),
	THREE_MONTHS(3, DateTimeUnit.MONTH),
	SIX_MONTHS(6, DateTimeUnit.MONTH),
	YEAR(1, DateTimeUnit.YEAR);

	/** Locations recorded before this are older than this age. */
	fun cutoff(now: Instant, timeZone: TimeZone): Instant = now.minus(amount, unit, timeZone)
}

/**
 * What deleting the locations older than a [LocationAge] removes, taken when its dialog opens.
 * Confirming deletes before this [cutoff], so exactly the [count] the dialog showed, even if the
 * dialog stayed open while locations aged past it.
 */
data class OlderThan(val cutoff: Instant, val count: Int)
