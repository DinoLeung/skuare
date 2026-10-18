package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.WorldTimezoneData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DstSettingsTest {
	@Test
	fun separatesKnownAndUnknownBits() {
		val settings = DstSettings.fromByte(0x07)

		assertTrue(settings.enable)
		assertTrue(settings.auto)
		assertEquals(0x04, settings.unknownBits)
		assertEquals(0x07, settings.byte.toInt())
	}

	@Test
	fun preservesUnsignedUnknownBits() {
		val settings = DstSettings.fromByte(0xF4.toByte())

		assertFalse(settings.enable)
		assertFalse(settings.auto)
		assertEquals(0xF4, settings.unknownBits)
		assertEquals(0xF4.toByte(), settings.byte)
	}

	@Test
	fun autoForNonDstZoneKeepsUnknownBits() {
		val settings = DstSettings(enable = true, auto = false, unknownBits = 0x04)
			.autoFor(WorldTimezoneData.getValue(281)) // Tokyo does not observe DST

		assertFalse(settings.enable)
		assertTrue(settings.auto)
		assertEquals(0x04, settings.unknownBits)
		assertEquals(0x06, settings.byte.toInt())
	}
}
