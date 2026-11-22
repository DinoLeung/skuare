package xyz.d1n0.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateFormatTest {
	@Test
	fun formatsFiveDecimalsWithADot() {
		assertEquals("22.27930", formatCoordinate(22.2793))
		assertEquals("114.16281", formatCoordinate(114.162812))
		assertEquals("-33.86882", formatCoordinate(-33.868820))
		assertEquals("0.00001", formatCoordinate(0.000006))
	}

	@Test
	fun roundsToTheNearestFifthDecimal() {
		assertEquals("1.00000", formatCoordinate(0.999996))
		assertEquals("-180.00000", formatCoordinate(-179.999999))
	}

	@Test
	fun zeroHasNoSign() {
		assertEquals("0.00000", formatCoordinate(0.0))
		assertEquals("0.00000", formatCoordinate(-0.000001))
	}
}
