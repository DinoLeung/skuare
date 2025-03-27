package xyz.d1n0.constant

enum class Command(val value: Int) {
	CONNECT_REASON(0x10),
	WATCH_NAME(0x23),
	CONNECTION_SETTINGS(0x11),
	WATCH_SETTINGS(0x13),
	CLOCK(0x1D),
	TIMEZONE_NAME(0x1F),
	TIMEZONE_INFO(0x1E),
	// somethingRelatedToTimezone(0x24), maybe it's not important
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
 *
 * CLASS_WATCH_FEATURE_SERVICE_LOCATION_AND_RADIO_INFORMATION
 * 240001C04170DEA465A5764061561B10921E7B00
 * 2401014049C00000000000000000000000000000
 * 240201403647C84B5DCC64405C8AD71F36262D03
 * 2403014041D841355475A3406176227D028A1E02
 * 240401404BFA04189374BCC0098193B3A68B1A04
 * 24050140445B6FD21FF2E5C0528061CFFEB07501
 *
 * 0 -> packet type
 * 1 -> clock index
 * 2–9 -> 8‑byte big‑endian double latitude
 * 10–17 -> 8‑byte big‑endian double longitude
 * 18 -> radio id
 */

