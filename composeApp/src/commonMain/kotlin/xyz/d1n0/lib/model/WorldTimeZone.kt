package xyz.d1n0.lib.model

import xyz.d1n0.lib.helper.toByteArray

data class WorldTimezone(
	override val timeZone: String,
	val country: String,
	val city: String,
	val longitude: Double,
	val latitude: Double,
	override val cityName: String,
	override val cityCode: String?,
	override val identifier: Short,
	override val offset: Double,
	override val dstDiff: Double,
	override val dstRules: Byte,
) : Timezone() {
	override val coordinatesBytes: ByteArray
		get() = byteArrayOf(
			*latitude.toByteArray(),
			*longitude.toByteArray(),
		)
}
