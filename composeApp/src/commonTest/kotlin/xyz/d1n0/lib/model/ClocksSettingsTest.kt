package xyz.d1n0.lib.model

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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

	@Test
	fun rejectsIdInNeitherTable() {
		assertFailsWith<IllegalArgumentException> {
			ClocksSettings().parseClocksPacket(unknownIdPacket)
		}
	}
}
