package xyz.d1n0.lib.constant

enum class OpCode(val byte: Byte) {
	CONNECT_REASON(0x10),
	CONNECTION_SETTINGS(0x11),
	WATCH_SETTINGS(0x13),
	WATCH_NAME(0x23),
	APP_INFO(0x22),
	WATCH_CONDITION(0x28),
//	CLASS_WATCH_FEATURE_SERVICE_MODULE_ID(0x26),
//	CLASS_WATCH_FEATURE_SERVICE_VERSION_INFORMATION(0x20),

	CLOCK(0x1D),
	CURRENT_TIME(0x09),

	TIMEZONE_NAME(0x1F),
	TIMEZONE_CONFIG(0x1E),
	TIMEZONE_LOCATION_RADIO_ID(0x24),

	ALARM_A(0x15),
	ALARM_B(0x16),

	TIMER(0x18),

	REMINDER_TITLE(0x30),
	REMINDER_CONFIG(0x31),

	ERROR(0xFF.toByte());
//    currentTimeManager(0x39),????

	// ECB-30
//    cmdSetTimeMode(0x47),
//    findPhone(0x0A);

	companion object {
		fun fromByte(byte: Byte) = entries.firstOrNull { it.byte == byte }
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
 * 28131900
 * [1] battery level, on gmwb5000 and gwb5600 full charged value is 0x13, bottom value is 0x09?
 * [2] temperature
 * [3] flash status(probably not useed on squares)
 */
