package xyz.d1n0.constant

enum class Command(val value: Int) {
	CONNECT_REASON(0x10),
	WATCH_NAME(0x23),
	CONNECTION_SETTINGS(0x11),
	WATCH_SETTINGS(0x13),
	CLOCK(0x1D),
	TIMEZONE_NAME(0x1F),
	TIMEZONE_CONFIG(0x1E),
	TIMEZONE_LOCATION_RADIO_ID(0x24),
	CURRENT_TIME(0x09),

	APP_INFO(0x22),
	WATCH_CONDITION(0x28),

	ALARM_A(0x15),
	ALARM_B(0x16),
    TIMER(0x18),

//    reminderTitle(0x30),
//    reminderTime(0x31),
	ERROR(0xFF);
//    currentTimeManager(0x39),????

	// ECB-30
//    cmdSetTimeMode(0x47),
//    findPhone(0x0A);

	companion object {
		//        fun fromValue(code: Int) = entries.firstOrNull { it.value == code } ?: error("Unknown command code: $code")
		fun fromValue(code: Int) = entries.firstOrNull { it.value == code }
	}
}

/**
 * CLASS_WATCH_FEATURE_SERVICE_MODULE_ID
 * 264CBB5847886A0E79B7F400001539FFFF
 *
 * CLASS_WATCH_FEATURE_SERVICE_VERSION_INFORMATION
 * 2022070101000000000000000101000000000000
 *
 * CLASS_WATCH_FEATURE_SERVICE_WATCH_CONDITION
 * 28131E00
 */
