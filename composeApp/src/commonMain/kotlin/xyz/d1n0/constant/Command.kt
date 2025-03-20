package xyz.d1n0.constant

enum class Command(val value: Int) {
	CONNECT_REASON(0x10),
	WATCH_NAME(0x23),
	AUTO_SYNC_SETTINGS(0x11),
	WATCH_SETTINGS(0x13),
	CLOCK(0x1D),
	TIMEZONE_NAME(0x1F),
	TIMEZONE_INFO(0x1E),
	// somethingRelatedToTimezone(0x24), maybe it's not important
	CURRENT_TIME(0x09),

	APP_INFO(0x22),
	WATCH_CONDITION(0x28),
	//    settingForAlm(0x15),
//    settingForAlm2(0x16),
//    settingForBasic(0x13),
//    currentTimeManager(0x39),
//    reminderTitle(0x30),
//    reminderTime(0x31),
//    timer(0x18),
	ERROR(0xFF);

	// ECB-30
//    cmdSetTimeMode(0x47),
//    findPhone(0x0A);

	companion object {
		//        fun fromValue(code: Int) = entries.firstOrNull { it.value == code } ?: error("Unknown command code: $code")
		fun fromValue(code: Int) = entries.firstOrNull { it.value == code }
	}
}
