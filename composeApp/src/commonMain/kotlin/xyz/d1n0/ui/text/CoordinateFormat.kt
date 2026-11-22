package xyz.d1n0.ui.text

import kotlin.math.abs
import kotlin.math.roundToLong

private const val COORDINATE_DECIMALS = 5
private const val COORDINATE_SCALE = 100_000L

/**
 * A latitude or longitude in decimal degrees with 5 decimals (about 1 m), e.g. -33.86882.
 *
 * Always with a dot and no grouping, whatever the language: it's the format map apps and search
 * boxes read, and a decimal comma would clash with the comma between latitude and longitude.
 */
fun formatCoordinate(degrees: Double): String {
	val scaled = (abs(degrees) * COORDINATE_SCALE).roundToLong()
	val whole = scaled / COORDINATE_SCALE
	val fraction = (scaled % COORDINATE_SCALE).toString().padStart(COORDINATE_DECIMALS, '0')
	// no "-0.00000" for a value that rounds to zero
	val sign = if (degrees < 0 && scaled != 0L) "-" else ""
	return "$sign$whole.$fraction"
}
