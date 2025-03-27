package xyz.d1n0.model

import xyz.d1n0.constant.WorldTimeZoneData

data class WorldTimeZone(
	val country: String,
	val city: String,
	val longitude: Double,
	val latitude: Double,
	override val cityName: String,
	override val identifier: Int,
	override val offset: Double,
	override val dstDiff: Double,
	override val dstRules: Int
) : TimeZone() {
	companion object {
		/**
		 * Performs a case-insensitive search for the given keyword across all WorldTimeZone instances.
		 * Matches are found where the keyword is a substring of the city or country name.
		 * If the keyword is empty or contains only whitespace, all WorldTimeZone instances are returned.
		 * @param keyword The search term to look for in city and country names.
		 * @return A list of WorldTimeZone objects whose city or country names contain the keyword.
		 */
		fun fuzzySearch(keyword: String): List<WorldTimeZone> {
			val keywordLowerCase = keyword.trim().lowercase()
			if (keywordLowerCase.isEmpty()) return WorldTimeZoneData.values.toList()
			return WorldTimeZoneData.values.filter {
				it.city.lowercase().contains(keywordLowerCase) || it.country.lowercase().contains(keywordLowerCase)
			}
		}
	}
}
