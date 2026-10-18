package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchPreferencesTest {
	@Test
	fun zeroByteUsesEnabledDefaults() {
		val preferences = WatchPreferences.fromByte(0)

		assertFalse(preferences.is24HourTime)
		assertFalse(preferences.isToneMuted)
		assertTrue(preferences.autoBacklight)
		assertTrue(preferences.powerSaving)
		assertEquals(0, preferences.byte.toInt())
	}

	@Test
	fun parsesAndWritesAllFlags() {
		val preferences = WatchPreferences.fromByte(0x17)

		assertTrue(preferences.is24HourTime)
		assertTrue(preferences.isToneMuted)
		assertFalse(preferences.autoBacklight)
		assertFalse(preferences.powerSaving)
		assertEquals(0x17, preferences.byte.toInt())
	}

	@Test
	fun ignoresUnknownBitsWhenWriting() {
		val preferences = WatchPreferences.fromByte(0x40)

		assertEquals(0, preferences.byte.toInt())
	}
}
