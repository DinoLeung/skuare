package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class ClocksSettingsTest {
	// pinned holds Hong Kong as a world id (122 = 7A 00), clock 1 holds Adelaide as a home id (30335 = 7F 76)
	private val swappedPacket = "1D000103027A007F76FFFFFFFFFFFF".hexToByteArray()

	// pinned holds Adelaide as a home id, clock 1 holds UTC as a world id
	private val normalPacket = "1D000103027F760000FFFFFFFFFFFF".hexToByteArray()

	// pinned holds 0x1234, which is in neither table
	private val unknownIdPacket = "1D00010302341200 00FFFFFFFFFFFF".replace(" ", "").hexToByteArray()

	@Test
	fun parsesSwappedClocksByIdNotByPosition() {
		val clocks = ClocksSettings().parseClocksPacket(swappedPacket)

		val pinned = assertIs<WorldClock>(clocks.pinnedClock)
		assertEquals("Asia/Hong_Kong", pinned.timeZone.timeZone)
		assertEquals(122, pinned.timeZone.identifier.toInt())

		val first = assertIs<HomeClock>(clocks.clocks[0])
		assertEquals("Australia/Adelaide", first.timeZone.timeZone)
		assertEquals(30335, first.timeZone.identifier.toInt())
	}

	@Test
	fun keepsIdentifierBytesFromTheWatch() {
		val clocks = ClocksSettings().parseClocksPacket(swappedPacket)

		assertContentEquals(swappedPacket.sliceArray(5..6), clocks.pinnedClock?.timeZone?.identifierBytes)
		assertContentEquals(swappedPacket.sliceArray(7..8), clocks.clocks[0]?.timeZone?.identifierBytes)
	}

	@Test
	fun parsesNormalClocks() {
		val clocks = ClocksSettings().parseClocksPacket(normalPacket)

		assertEquals(30335, assertIs<HomeClock>(clocks.pinnedClock).timeZone.identifier.toInt())
		assertEquals(0, assertIs<WorldClock>(clocks.clocks[0]).timeZone.identifier.toInt())
	}

	// captured from a GW-B5600 swapped on the watch: pinned Hong Kong (world), clock 2 Adelaide (home)
	private val watchPackets = listOf(
		"1D000102027A000000FFFFFFFFFFFF",
		"1D020303027F761901FFFFFFFFFFFF",
		"1D040503035F003F00FFFFFFFFFFFF",
	).map { it.hexToByteArray() }

	private fun parseAll(packets: List<ByteArray>) =
		packets.fold(ClocksSettings()) { clocks, packet -> clocks.parseClocksPacket(packet) }

	@Test
	fun pinSwapsWholeClocksWithThePinnedClock() {
		val clocks = parseAll(watchPackets).pin(1)

		val pinned = assertIs<HomeClock>(clocks.pinnedClock)
		assertEquals("Australia/Adelaide", pinned.timeZone.timeZone)
		val swapped = assertIs<WorldClock>(clocks.clocks[1])
		assertEquals("Asia/Hong_Kong", swapped.timeZone.timeZone)

		// DST settings move with their clock: Adelaide keeps 03, Hong Kong keeps 02
		assertEquals(
			listOf(
				"1D000103027F760000FFFFFFFFFFFF",
				"1D020302027A001901FFFFFFFFFFFF",
				"1D040503035F003F00FFFFFFFFFFFF",
			),
			clocks.clocksPackets.map { it.toHexString(HexFormat.UpperCase) },
		)
	}

	@Test
	fun pinningBackRestoresTheOriginalClocks() {
		val clocks = parseAll(watchPackets)

		assertEquals(clocks, clocks.pin(1).pin(1))
	}

	@Test
	fun pinRequiresInitializedClocks() {
		assertFailsWith<IllegalArgumentException> { ClocksSettings().pin(0) }
	}

	// from a GW-BX5600 (logcat 2026-10-18): pinned Adelaide (home), Hong Kong, Tokyo, then the
	// unused slot 3 with time zone ID 0000
	private val threeClockPackets = listOf(
		"1D000107067F767A00FFFFFFFFFFFF",
		"1D0203020019010000FFFFFFFFFFFF",
	).map { it.hexToByteArray() }

	private fun parseThreeClocks() =
		threeClockPackets.fold(ClocksSettings.empty(3)) { clocks, packet -> clocks.parseClocksPacket(packet) }

	@Test
	fun emptyIsSizedByClockCount() {
		assertEquals(6, ClocksSettings.empty(6).clockCount)
		assertEquals(3, ClocksSettings.empty(3).clockCount)
		assertFalse(ClocksSettings.empty(3).isInitialized)
	}

	@Test
	fun parsesThreeClocksAndIgnoresTheUnusedSlot() {
		val clocks = parseThreeClocks()

		assertTrue(clocks.isInitialized)
		assertEquals(2, clocks.clocks.size)
		assertEquals("Australia/Adelaide", assertIs<HomeClock>(clocks.pinnedClock).timeZone.timeZone)
		assertEquals("Asia/Hong_Kong", assertIs<WorldClock>(clocks.clocks[0]).timeZone.timeZone)
		assertEquals("Asia/Tokyo", assertIs<WorldClock>(clocks.clocks[1]).timeZone.timeZone)
	}

	@Test
	fun threeClocksAreNotInitializedByTheFirstPairOnly() {
		assertFalse(ClocksSettings.empty(3).parseClocksPacket(threeClockPackets[0]).isInitialized)
	}

	@Test
	fun writesThreeClocksLikeTheOfficialApp() {
		// the official app's write in world_clock_write_bx: the 0x04 flag bit kept on clocks 0
		// and 1 (07 / 06), Tokyo with DST 02, then the unused slot as DST 00 and ID FF FF
		assertEquals(
			listOf(
				"1D000107067F767A00FFFFFFFFFFFF",
				"1D020302001901FFFFFFFFFFFFFFFF",
			),
			parseThreeClocks().clocksPackets.map { it.toHexString(HexFormat.UpperCase) },
		)
	}

	@Test
	fun writesTimeZoneConfigsLikeTheOfficialApp() {
		assertEquals(
			listOf("1E007F76260404", "1E017A00200400", "1E021901240400"),
			parseThreeClocks().timeZoneConfigPackets.map { it.toHexString(HexFormat.UpperCase) },
		)
	}

	@Test
	fun writesCityNamesAndShortNamesLikeTheOfficialApp() {
		val clocks = parseThreeClocks()
		val padding = "00".repeat(18)
		fun nameRecord(position: String, name: String) =
			("1F$position" + name.encodeToByteArray().toHexString(HexFormat.UpperCase) + padding).take(40)

		assertEquals(
			listOf(nameRecord("00", "ADELAIDE"), nameRecord("01", "HONG KONG"), nameRecord("02", "TOKYO")),
			clocks.timeZoneNamePackets.map { it.toHexString(HexFormat.UpperCase) },
		)
		assertEquals(
			listOf(nameRecord("06", "ADL"), nameRecord("07", "HKG"), nameRecord("08", "TYO")),
			clocks.timeZoneShortNamePackets.map { it.toHexString(HexFormat.UpperCase) },
		)
	}

	@Test
	fun writesCoordinatesAndRadioIdsLikeTheOfficialApp() {
		val packets = parseThreeClocks().coordinatesRadioIdPackets.map { it.toHexString(HexFormat.UpperCase) }

		// Tokyo, the city changed in world_clock_write_bx, is the same as the official app's write
		assertEquals("2402014041D841355475A3406176227D028A1E02", packets[2])
		// The other two differ from the official app, which wrote
		// 240001C04170DDF6C0BC1D4061561BA908A26600 and 240101403647C84B5DCC64405C8AD71F36262D05:
		// home cities get the coordinates of the world city with the same name (the app has other
		// ones for home Adelaide), and the radio ID comes from the UTC offset (03 for Hong Kong,
		// the app wrote 05). Same on the classic route.
		assertEquals("240001C04176DD052934AD40615332DF505D1000", packets[0])
		assertEquals("240101403647C84B5DCC64405C8AD71F36262D03", packets[1])
	}

	@Test
	fun unknownDstBitsStayInTheirPosition() {
		// Tokyo (02) pinned: it takes the 0x04 bit of position 0, Adelaide moves to position 2 without it
		val clocks = parseThreeClocks().pin(1)

		assertEquals(0x06, clocks.pinnedClock?.dstSettings?.byte?.toInt())
		assertEquals("Asia/Tokyo", clocks.pinnedClock?.timeZone?.timeZone)
		assertEquals(0x03, clocks.clocks[1]?.dstSettings?.byte?.toInt())
		assertEquals("Australia/Adelaide", clocks.clocks[1]?.timeZone?.timeZone)
	}

	@Test
	fun sixClocksStillNeedAllThreePairs() {
		assertFalse(parseAll(watchPackets.take(2)).isInitialized)
		assertTrue(parseAll(watchPackets).isInitialized)
		assertEquals(3, parseAll(watchPackets).clocksPackets.size)
	}

	@Test
	fun rejectsPositionsPastTheLastSlot() {
		assertFailsWith<IllegalArgumentException> {
			ClocksSettings().parseClocksPacket("1D060703027F760000FFFFFFFFFFFF".hexToByteArray())
		}
	}

	@Test
	fun rejectsIdInNeitherTable() {
		assertFailsWith<IllegalArgumentException> {
			ClocksSettings().parseClocksPacket(unknownIdPacket)
		}
	}
}
