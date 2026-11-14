package xyz.d1n0.lib.model

/**
 * What a watch has, from a known [WatchModel] or a user-set [CustomWatchModel].
 *
 * @property clockCount Clocks on the watch, the pinned clock included.
 * @property reminderCount Reminders on the watch, 0 if it has none.
 * @property hasFontStyle Whether the watch has a font style setting, see [xyz.d1n0.lib.constant.FontStyle].
 * @property hasShortCityNames Whether the watch stores a 3-letter city code next to each city
 *           name, see [ClocksSettings.timeZoneShortNamePackets].
 * @property hasWeekdayLanguage Whether the weekday can be changed, see
 *           [xyz.d1n0.lib.constant.WeekdayLanguage]. Watches without it always report English in
 *           [WatchSettings] and ignore the byte when written.
 * @property hasSnoozeAlarm Whether the fifth alarm is a snooze alarm. Without it the fifth alarm
 *           is a normal one, in the same slot of the same packets, see [AlarmsSettings].
 * @property hasTimeZoneLocation Whether the clocks' coordinates and radio IDs are written, see
 *           [xyz.d1n0.lib.constant.OpCode.TIMEZONE_LOCATION_RADIO_ID]. The official app never
 *           sends them to watches without it. Not the same as having a radio receiver, the
 *           DW-B5600 has none but takes them.
 * @property canAdjustHands Whether the watch has hands to adjust (home position correction).
 */
interface WatchFeatures {
	val clockCount: Int
	val reminderCount: Int
	val hasFontStyle: Boolean
	val hasShortCityNames: Boolean
	val hasWeekdayLanguage: Boolean
	val hasSnoozeAlarm: Boolean
	val hasTimeZoneLocation: Boolean
	val canAdjustHands: Boolean
}
