package xyz.d1n0.lib.constant

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TimezoneDataTest {
	// aliases and short ids, e.g. Asia/Hanoi or EST, are missing from some time zone databases
	@Test
	fun everyTimeZoneIsAKnownIanaId() {
		val ids = WorldTimezoneData.values.map { it.timeZone } + HomeTimezoneData.values.map { it.timeZone }

		assertEquals(emptyList(), ids.distinct().filter { runCatching { TimeZone.of(it) }.isFailure })
	}
}
