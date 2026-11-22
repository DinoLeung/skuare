package xyz.d1n0.phone

import xyz.d1n0.ui.text.formatCoordinate

/**
 * Builds a link that opens a position in the phone's map app, for
 * [androidx.compose.ui.platform.UriHandler]. Each platform binds the format its map apps read.
 * Nothing is fetched by skuare, the map app does that.
 */
interface MapLinkBuilder {
	/** @param label Shown on the pin, e.g. the date of the press. */
	fun link(point: GeoPoint, label: String): String
}

/**
 * A geo URI (RFC 5870) with a query, which Android map apps (Google Maps, OsmAnd, Organic Maps)
 * open with a labelled pin, e.g. geo:22.27930,114.16281?q=22.27930,114.16281(label).
 */
object GeoUriMapLinks : MapLinkBuilder {
	override fun link(point: GeoPoint, label: String): String {
		val position = "${formatCoordinate(point.latitude)},${formatCoordinate(point.longitude)}"
		return "geo:$position?q=$position(${percentEncode(label)})"
	}
}

/**
 * An Apple Maps link, e.g. maps://?ll=22.27930,114.16281&q=label, ll places the pin and q names it.
 */
object AppleMapsLinks : MapLinkBuilder {
	override fun link(point: GeoPoint, label: String): String =
		"maps://?ll=${formatCoordinate(point.latitude)},${formatCoordinate(point.longitude)}" +
			"&q=${percentEncode(label)}"
}

/** Percent-encodes everything but RFC 3986's unreserved characters, as UTF-8. */
internal fun percentEncode(text: String): String = buildString {
	text.encodeToByteArray().forEach { byte ->
		val char = byte.toInt().toChar()
		if (byte >= 0 && (char.isLetterOrDigit() || char in "-._~")) append(char)
		else append('%').append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
	}
}
