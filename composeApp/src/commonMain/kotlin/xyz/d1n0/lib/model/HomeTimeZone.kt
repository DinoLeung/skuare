package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.WorldTimezoneData

data class HomeTimezone(
	override val timeZone: String,
	override val cityName: String,
	override val cityCode: String?,
	override val identifier: Short,
	override val offset: Double,
	override val dstDiff: Double,
	override val dstRules: Byte,
) : Timezone() {
	private fun getEquivalentWorldTimezone() =
		WorldTimezoneData.values.find { it.cityName == this.cityName }

	/**
	 * Retrieves the coordinates associated with this HomeTimezone as a ByteArray.
	 * It finds the equivalent WorldTimezone based on the city name, then returns its coordinates.
	 *
	 * @return A ByteArray containing the coordinates of the corresponding WorldTimezone if found;
	 *         otherwise, a 16-byte array filled with zeros.
	 */
	override val coordinatesBytes: ByteArray
		get() = getEquivalentWorldTimezone()?.coordinatesBytes ?: ByteArray(16) { 0 }
}
