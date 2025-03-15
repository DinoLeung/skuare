package xyz.d1n0.model

import xyz.d1n0.constant.HomeTimeZoneData

data class HomeTimeZone(
	val timeZone: String,
	override val cityName: String,
	override val identifier: Int,
	override val offset: Double,
	override val dstDiff: Double,
	override val dstRules: Int
) : TimeZone() {
	companion object {
		/**
		 * Perform a fuzzy search on the list of HomeTimeZones by matching the keyword with the `timeZone` field.
		 * @param keyword The search keyword.
		 * @return A list of HomeTimeZone objects matching the search keyword.
		 */
		fun fuzzySearch(keyword: String): List<HomeTimeZone> {
			val keywordLowerCase = keyword.trim().lowercase()
			if (keywordLowerCase.isEmpty()) return HomeTimeZoneData.values.toList()
			return HomeTimeZoneData.values.filter { it.timeZone.lowercase().contains(keywordLowerCase) }
		}
	}
}
