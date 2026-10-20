package xyz.d1n0.lib.model

/**
 * What a watch model has, decided from the name it advertises, see [fromAdvertisedName].
 * The watch name from [xyz.d1n0.lib.constant.OpCode.WATCH_NAME] can't be used for this, the user
 * can set it to anything.
 *
 * @property clockCount Clocks on the watch, the pinned clock included.
 * @property reminderCount Reminders on the watch, 0 if it has none.
 * @property hasFontStyle Whether the watch has a font style setting, see [xyz.d1n0.lib.constant.FontStyle].
 * @property hasShortCityNames Whether the watch stores a 3-letter city code next to each city
 *           name, see [ClocksSettings.timeZoneShortNamePackets].
 */
enum class WatchModel(
	val clockCount: Int,
	val reminderCount: Int,
	val hasFontStyle: Boolean,
	val hasShortCityNames: Boolean,
) {
	CLASSIC_SQUARE(clockCount = 6, reminderCount = 5, hasFontStyle = false, hasShortCityNames = false), // GMW-B5000, GW-B5600
	MIP_SQUARE(clockCount = 3, reminderCount = 0, hasFontStyle = true, hasShortCityNames = true);      // GW-BX5600, GMW-BZ5000

	companion object {
		private val models = mapOf(
			"B5000" to CLASSIC_SQUARE,  // modules 3459, 3539
			"B5600" to CLASSIC_SQUARE,  // modules 3461, 3540
			"BX5600" to MIP_SQUARE,     // module 3578
			"BZ5000" to MIP_SQUARE,     // module 3575
		)

		/**
		 * Finds the model by exact match on the part after the last hyphen. The family prefix is
		 * ignored, so GMW-B5000 is matched as B5000. A trailing # only marks newer modules of the
		 * same model, e.g. CASIO GMW-B5000#.
		 *
		 * @param name Name the watch advertises, e.g. CASIO GW-BX5600.
		 * @return The model, or null if the name is unknown or null.
		 */
		fun fromAdvertisedName(name: String?): WatchModel? =
			name?.removeSuffix("#")?.substringAfterLast('-')?.let { models[it] }
	}
}
