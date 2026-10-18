package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.HomeTimezoneData
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.constant.WorldTimezoneData
import xyz.d1n0.lib.helper.toByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class ClockTimezoneTest {
	private val utc = WorldTimezoneData.getValue(0)
	private val adelaideHome = HomeTimezoneData.getValue(30335)
	private val hongKong = WorldTimezoneData.getValue(122)

	@Test
	fun timezoneBuildsProtocolFields() {
		assertContentEquals(byteArrayOf(0, 0), utc.identifierBytes)
		assertContentEquals(byteArrayOf(0, 0, 0, 0, 0), utc.bytes)
		assertEquals(18, utc.cityNameBytes.size)
		assertContentEquals(ByteArray(18), utc.cityCodeBytes)
	}

	@Test
	fun timezoneScalesQuarterHourOffsets() {
		val timezone = WorldTimezone(
			timeZone = "America/St_Johns",
			country = "Canada",
			city = "St. John's",
			longitude = -52.7126,
			latitude = 47.5615,
			cityName = "ST. JOHN'S",
			cityCode = "YYT",
			identifier = 42,
			offset = -3.5,
			dstDiff = 1.0,
			dstRules = 7,
		)

		assertEquals((-14).toByte(), timezone.offsetByte)
		assertEquals(4.toByte(), timezone.dstOffsetByte)
		assertContentEquals(byteArrayOf(42, 0, -14, 4, 7), timezone.bytes)
		assertContentEquals(
			byteArrayOf(*timezone.latitude.toByteArray(), *timezone.longitude.toByteArray()),
			timezone.coordinatesBytes,
		)
	}

	@Test
	fun reportsIanaOffsetAndDstAtFixedInstants() {
		val summer = Instant.parse("2026-01-15T00:00:00Z")
		val winter = Instant.parse("2026-07-15T00:00:00Z")

		assertEquals(630, adelaideHome.currentOffsetMinutes(summer))
		assertEquals("+10:30", adelaideHome.ianaOffsetString(summer))
		assertTrue(adelaideHome.isExpectedDst(summer))
		assertEquals(570, adelaideHome.currentOffsetMinutes(winter))
		assertEquals("+9:30", adelaideHome.ianaOffsetString(winter))
		assertFalse(adelaideHome.isExpectedDst(winter))
	}

	@Test
	fun homeTimezoneUsesEquivalentWorldCoordinates() {
		val worldAdelaide = WorldTimezoneData.values.first { it.cityName == adelaideHome.cityName }

		assertContentEquals(worldAdelaide.coordinatesBytes, adelaideHome.coordinatesBytes)
	}

	@Test
	fun searchesWorldAndHomeTimezonesCaseInsensitively() {
		assertTrue(WorldTimezone.fuzzySearch("hong kong").any { it.identifier == hongKong.identifier })
		assertTrue(WorldTimezone.fuzzySearch("australia").any { it.city == "Adelaide" })
		assertTrue(HomeTimezone.fuzzySearch("adelaide").any { it.identifier == adelaideHome.identifier })
		assertEquals(WorldTimezoneData.size, WorldTimezone.fuzzySearch("  ").size)
		assertEquals(HomeTimezoneData.size, HomeTimezone.fuzzySearch("").size)
	}

	@Test
	fun clockFactorySelectsTableFromIdentifier() {
		val dst = DstSettings(enable = false, auto = true)

		assertIs<WorldClock>(Clock.fromTimeZoneId(hongKong.identifier, dst))
		assertIs<HomeClock>(Clock.fromTimeZoneId(adelaideHome.identifier, dst))
		assertFailsWith<IllegalArgumentException> { Clock.fromTimeZoneId(12345, dst) }
	}

	@Test
	fun replacingUnknownDstBitsKeepsClockTypeAndTimezone() {
		val original = WorldClock(hongKong, DstSettings(enable = true, auto = false, unknownBits = 0x04))
		val changed = assertIs<WorldClock>(original.withUnknownDstBits(0x20))

		assertEquals(hongKong, changed.timeZone)
		assertTrue(changed.dstSettings.enable)
		assertFalse(changed.dstSettings.auto)
		assertEquals(0x20, changed.dstSettings.unknownBits)
	}

	@Test
	fun currentTimePacketHasProtocolEnvelope() {
		val clock = WorldClock(utc, DstSettings(enable = false, auto = false))
		val packet = clock.getCurrentDateTimePacket()

		assertEquals(11, packet.size)
		assertEquals(OpCode.CURRENT_TIME.byte, packet.first())
		assertEquals(0x01, packet.last().toInt())
	}
}
