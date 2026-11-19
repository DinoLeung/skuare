package xyz.d1n0.ui.text

import xyz.d1n0.lib.constant.HomeTimezoneData
import xyz.d1n0.lib.constant.WorldTimezoneData
import xyz.d1n0.resources.Res
import xyz.d1n0.resources.allStringResources
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimezoneLabelsTest {
	@Test
	fun everyTimeZoneHasAGeneratedCityName() {
		val missing = (HomeTimezoneData.values + WorldTimezoneData.values)
			.map { it.cityNameKey }
			.filter { it == null || it !in Res.allStringResources }
		// run the generators in data/ after adding a city to their CSVs
		assertTrue(missing.isEmpty(), "No generated name for $missing")
	}

	@Test
	fun homeAndWorldNamesHaveTheirOwnKeys() {
		assertEquals("home_tz_30002", HomeTimezoneData.getValue(30002).cityNameKey)
		assertEquals("world_tz_281", WorldTimezoneData.getValue(281).cityNameKey)
	}
}
