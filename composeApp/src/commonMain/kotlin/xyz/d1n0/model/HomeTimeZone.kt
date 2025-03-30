package xyz.d1n0.model

import xyz.d1n0.constant.HomeTimezoneData
import xyz.d1n0.constant.WorldTimezoneData

data class HomeTimezone(
	val timeZone: String,
	override val cityName: String,
	override val identifier: Short,
	override val offset: Double,
	override val dstDiff: Double,
	override val dstRules: Byte
) : Timezone() {
	companion object {
		/**
		 * Perform a fuzzy search on the list of HomeTimeZones by matching the keyword with the `timeZone` field.
		 * @param keyword The search keyword.
		 * @return A list of HomeTimeZone objects matching the search keyword.
		 */
		fun fuzzySearch(keyword: String): List<HomeTimezone> {
			val keywordLowerCase = keyword.trim().lowercase()
			if (keywordLowerCase.isEmpty()) return HomeTimezoneData.values.toList()
			return HomeTimezoneData.values.filter { it.timeZone.lowercase().contains(keywordLowerCase) }
		}
	}

	private fun getEquivalentWorldTimezone() = WorldTimezoneData.values.find { it.cityName == this.cityName }

	/**
	 * Retrieves the coordinates associated with this HomeTimezone as a ByteArray.
	 * It finds the equivalent WorldTimezone based on the city name, then returns its coordinates.
	 *
	 * @return A ByteArray containing the coordinates of the corresponding WorldTimezone if found;
	 *         otherwise, a 16-byte array filled with zeros.
	 */
	override val coordinatesBytes: ByteArray
		get() = getEquivalentWorldTimezone()
			?.let { it.coordinatesBytes }
			?: ByteArray(16) { 0 }
}
