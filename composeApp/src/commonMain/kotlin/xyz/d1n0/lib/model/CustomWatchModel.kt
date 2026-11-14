package xyz.d1n0.lib.model

/**
 * Features the user set for a watch skuare doesn't recognise, see [WatchModel.fromAdvertisedName].
 * The defaults are the minimal profile: a home clock and one world clock, nothing optional. The
 * fifth alarm stays a snooze alarm ([hasSnoozeAlarm]), as on every watch before custom models.
 *
 * A custom watch that adjusts hands ([canAdjustHands]) uses the GA-B2100's hand groups, see
 * [xyz.d1n0.lib.constant.HandGroup], the watch doesn't say which hands it has.
 *
 * @throws IllegalArgumentException if [clockCount] is not in 1..6 or [reminderCount] is not in 0..5.
 */
data class CustomWatchModel(
	override val clockCount: Int = 2,
	override val reminderCount: Int = 0,
	override val hasFontStyle: Boolean = false,
	override val hasShortCityNames: Boolean = false,
	override val hasWeekdayLanguage: Boolean = false,
	override val hasSnoozeAlarm: Boolean = true,
	override val hasTimeZoneLocation: Boolean = false,
	override val canAdjustHands: Boolean = false,
) : WatchFeatures {
	init {
		// clock positions are 0..5, see ClocksSettings
		require(clockCount in CLOCK_COUNT_RANGE) { "Clock count must be in $CLOCK_COUNT_RANGE" }
		require(reminderCount in REMINDER_COUNT_RANGE) { "Reminder count must be in $REMINDER_COUNT_RANGE" }
	}

	companion object {
		val CLOCK_COUNT_RANGE = 1..6
		val REMINDER_COUNT_RANGE = 0..5

		/**
		 * A custom copy of [features], e.g. to keep a watch paired before custom models on the
		 * [WatchModel.CLASSIC_SQUARE] features it had.
		 */
		fun from(features: WatchFeatures) = CustomWatchModel(
			clockCount = features.clockCount,
			reminderCount = features.reminderCount,
			hasFontStyle = features.hasFontStyle,
			hasShortCityNames = features.hasShortCityNames,
			hasWeekdayLanguage = features.hasWeekdayLanguage,
			hasSnoozeAlarm = features.hasSnoozeAlarm,
			hasTimeZoneLocation = features.hasTimeZoneLocation,
			canAdjustHands = features.canAdjustHands,
		)
	}
}

/**
 * Decides what a watch has. A known advertised name always wins, so a watch that gets supported
 * later drops its custom model by itself.
 *
 * @param advertisedName Name the watch advertises, e.g. CASIO GW-BX5600.
 * @param customModel The custom model stored for the watch, null if none, e.g. it isn't paired yet
 *        or was paired before custom models existed.
 * @return The known model, else [customModel], else the default [CustomWatchModel].
 */
fun resolveWatchModel(advertisedName: String?, customModel: CustomWatchModel?): WatchFeatures =
	WatchModel.fromAdvertisedName(advertisedName) ?: customModel ?: CustomWatchModel()

/**
 * Whether a watch using [current] should switch to [next], see [Watch.useCustomModel]. Only a
 * watch that is already custom can switch, a known [WatchModel] is fixed by its advertised name.
 *
 * @return false if [current] already is [next].
 * @throws IllegalStateException if [current] is a known [WatchModel].
 */
internal fun shouldSwitchToCustomModel(current: WatchFeatures, next: CustomWatchModel): Boolean {
	check(current is CustomWatchModel) { "$current is a known model, its features can't be changed" }
	return current != next
}
