package xyz.d1n0.lib.constant

/**
 * @property mask Bit of the day in the days of week byte of a reminder config.
 */
enum class ReminderDayOfWeek(val mask: Int) {
	SUNDAY(0b00000001),
	MONDAY(0b00000010),
	TUESDAY(0b00000100),
	WEDNESDAY(0b00001000),
	THURSDAY(0b00010000),
	FRIDAY(0b00100000),
	SATURDAY(0b01000000);

	companion object {
		fun daysFromByte(byte: Byte): Set<ReminderDayOfWeek> =
			entries.filter { byte.toInt() and it.mask != 0 }.toSet()

		fun byteFromDays(days: Set<ReminderDayOfWeek>): Byte =
			days.fold(0) { acc, day -> acc or day.mask }.toByte()
	}
}
