package xyz.d1n0.lib.model

/**
 * A watch model skuare knows, decided from the name it advertises, see [fromAdvertisedName].
 * The watch name from [xyz.d1n0.lib.constant.OpCode.WATCH_NAME] can't be used for this, the user
 * can set it to anything. Other watches use a [CustomWatchModel].
 *
 * See [WatchFeatures] for what each property means.
 */
enum class WatchModel(
	override val clockCount: Int,
	override val reminderCount: Int,
	override val hasFontStyle: Boolean,
	override val hasShortCityNames: Boolean,
	override val hasWeekdayLanguage: Boolean,
) : WatchFeatures {
	CLASSIC_SQUARE( // MRG-5000, GMW-B5000, GW-B5600
		clockCount = 6,
		reminderCount = 5,
		hasFontStyle = false,
		hasShortCityNames = false,
		hasWeekdayLanguage = true,
	),
	MIP_SQUARE( // GW-BX5600, GMW-BZ5000
		clockCount = 3,
		reminderCount = 0,
		hasFontStyle = true,
		hasShortCityNames = true,
		hasWeekdayLanguage = false,
	),
	DW_SQUARE( // DW-B5600
		clockCount = 2,
		reminderCount = 0,
		hasFontStyle = false,
		hasShortCityNames = false,
		hasWeekdayLanguage = true,
	);

	companion object {
		private val models = mapOf(
			"MRG-B5000" to CLASSIC_SQUARE,  // module 3501
			"GMW-B5000" to CLASSIC_SQUARE,  // modules 3459, 3539
			"GW-B5600" to CLASSIC_SQUARE,   // modules 3461, 3540
			"GW-BX5600" to MIP_SQUARE,      // module 3578
			"GMW-BZ5000" to MIP_SQUARE,     // module 3575
			"DW-B5600" to DW_SQUARE,        // module 3509
		)

		/**
		 * Finds the model by exact match on the full model after the last space, e.g. GW-BX5600 in
		 * CASIO GW-BX5600. A trailing # only marks newer modules of the same model, e.g.
		 * CASIO GMW-B5000#.
		 *
		 * @param name Name the watch advertises, e.g. CASIO GW-BX5600.
		 * @return The model, or null if the name is unknown or null.
		 */
		fun fromAdvertisedName(name: String?): WatchModel? =
			name?.removeSuffix("#")?.substringAfterLast(' ')?.let { models[it] }
	}
}
