package xyz.d1n0.lib.model

/**
 * What a watch has, from a known [WatchModel] or a user-set [CustomWatchModel].
 *
 * @property clockCount Clocks on the watch, the pinned clock included.
 * @property reminderCount Reminders on the watch, 0 if it has none.
 * @property hasFontStyle Whether the watch has a font style setting, see [xyz.d1n0.lib.constant.FontStyle].
 * @property hasShortCityNames Whether the watch stores a 3-letter city code next to each city
 *           name, see [ClocksSettings.timeZoneShortNamePackets].
 * @property hasWeekdayLanguage Whether the weekday language can be changed, see
 *           [xyz.d1n0.lib.constant.WeekdayLanguage]. Watches without it always report English in
 *           [WatchSettings] and ignore the byte when written.
 */
interface WatchFeatures {
	val clockCount: Int
	val reminderCount: Int
	val hasFontStyle: Boolean
	val hasShortCityNames: Boolean
	val hasWeekdayLanguage: Boolean
}
