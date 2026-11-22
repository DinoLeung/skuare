package xyz.d1n0.phone

import kotlin.test.Test
import kotlin.test.assertEquals

class MapLinkBuilderTest {
	private val point = GeoPoint(latitude = 22.2793, longitude = 114.16281)

	@Test
	fun androidGeoUriPinsThePositionWithALabel() {
		assertEquals(
			"geo:22.27930,114.16281?q=22.27930,114.16281(22%20Nov%202026%2C%2014%3A05)",
			GeoUriMapLinks.link(point, "22 Nov 2026, 14:05"),
		)
	}

	@Test
	fun appleMapsLinkPinsThePositionWithALabel() {
		assertEquals(
			"maps://?ll=22.27930,114.16281&q=22%20Nov%202026%2C%2014%3A05",
			AppleMapsLinks.link(point, "22 Nov 2026, 14:05"),
		)
	}

	@Test
	fun smallAndNegativeCoordinatesStayDecimal() {
		// Double.toString would give 1.0E-5, which no map app reads
		assertEquals(
			"maps://?ll=-0.00001,-151.20929&q=x",
			AppleMapsLinks.link(GeoPoint(-0.00001, -151.20929), "x"),
		)
	}

	@Test
	fun labelIsEncodedAsUtf8() {
		// a localized date, and the characters that would end the label or the query early
		assertEquals("2026%E5%B9%B411%E6%9C%8822%E6%97%A5", percentEncode("2026年11月22日"))
		assertEquals("%28a%29%26b%3Dc%23d", percentEncode("(a)&b=c#d"))
		assertEquals("AZaz09-._~", percentEncode("AZaz09-._~"))
	}
}
