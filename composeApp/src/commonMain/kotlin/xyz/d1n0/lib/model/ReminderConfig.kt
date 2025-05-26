package xyz.d1n0.lib.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import xyz.d1n0.lib.constant.ReminderBitmask
import xyz.d1n0.lib.constant.ReminderDayOfWeek
import xyz.d1n0.lib.constant.ReminderRecurrence
import xyz.d1n0.lib.helper.fromBcdByteArray
import xyz.d1n0.lib.helper.toBcdByteArray
import kotlinx.datetime.Clock as KotlinClock
import kotlinx.datetime.TimeZone as KotlinTimeZone

data class ReminderConfig(
	val enable: Boolean,
	val recurrence: ReminderRecurrence,
	var startDate: LocalDate,
	var endDate: LocalDate,
	val daysOfWeek: Set<ReminderDayOfWeek>,
) {
	companion object {
		fun fromBytes(bytes: ByteArray): ReminderConfig {
			require(bytes.size == 9) { "Reminder Config bytes must be exactly 9 bytes long, e.g. 11 25 03 25 25 03 25 12 00" }

			val startDate = runCatching {
				LocalDate.fromBcdByteArray(bytes.sliceArray(1..3))
			}.getOrElse {
				KotlinClock.System.now().toLocalDateTime(KotlinTimeZone.currentSystemDefault()).date
			}

			val endDate = runCatching {
				LocalDate.fromBcdByteArray(bytes.sliceArray(4..6))
			}.getOrElse {
				KotlinClock.System.now().toLocalDateTime(KotlinTimeZone.currentSystemDefault()).date
			}

			return ReminderConfig(
				enable = bytes[0].toInt() and ReminderBitmask.ENABLE != 0,
				recurrence = ReminderRecurrence.fromByte(bytes[0]),
				startDate = startDate,
				endDate = endDate,
				daysOfWeek = ReminderDayOfWeek.daysFromByte(bytes[7]),
			)
		}
	}

	// always ensure dates are either current date or later
	// not tested the results of writing a date that is before the current date
	init {
//		val today: LocalDate =
//			KotlinClock.System.now().toLocalDateTime(KotlinTimeZone.currentSystemDefault()).date
//		if (startDate < today) startDate = today
		if (endDate < startDate) endDate = startDate
//		if (recurrence == ReminderRecurrence.REPEAT_WEEKLY)
//			require(daysOfWeek.isNotEmpty()) { "Days of week cannot be empty" }
	}

	val bytes: ByteArray
		get() = byteArrayOf(
			(recurrence.byte.toInt() or (if (enable) 1 else 0)).toByte(),
			*startDate.toBcdByteArray(),
			*endDate.toBcdByteArray(),
			ReminderDayOfWeek.byteFromDays(daysOfWeek),
		).copyOf(9)

	val daysOfWeekDisplayString: String
		get() = daysOfWeek
			.takeIf { it.isNotEmpty() }
			?.map { it.abbreviatedName }
			?.joinToString(separator = ", ")
			?: "Never"
}
