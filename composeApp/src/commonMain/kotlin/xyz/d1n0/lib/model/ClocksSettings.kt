package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.ClocksUpdateSession
import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.helper.fromLittleEndianByteArray
import xyz.d1n0.lib.helper.replaceAt
import xyz.d1n0.lib.helper.requireIn

/**
 * The clocks on the watch, sized by [clocks]: [empty] makes one for a [WatchModel.clockCount].
 *
 * @property pinnedClock The clock shown on the watch's home screen, position 0.
 * @property clocks The other clocks, positions 1 and up.
 */
data class ClocksSettings(
	val pinnedClock: Clock? = null,
	val clocks: List<Clock?> = List(5) { null },
) {
	private val allClocks: List<Clock?>
		get() = listOf(
			pinnedClock,
			*clocks.toTypedArray(),
		)

	/**
	 * Clocks on the watch, the pinned clock included.
	 */
	val clockCount: Int get() = clocks.size + 1

	val isInitialized: Boolean get() = pinnedClock != null && clocks.all { it != null }

	/**
	 * Sets a clock for the specified position with the given time zone ID and DST status.
	 *
	 * The position only decides the slot. Whether the clock is a [HomeClock] or a [WorldClock]
	 * comes from the time zone ID, because the watch can swap a world clock into the pinned slot.
	 *
	 * Positions from [clockCount] up to 5 are slots the watch doesn't use and are ignored, e.g. a
	 * 3-clock watch sends position 3 with time zone ID 0000 in the same packet as clock 2.
	 *
	 * @param position The position of the clock, where 0 is pinnedClock and 1 and up are clocks.
	 * @param timeZoneId The ID of the time zone to assign to the specified clock position.
	 * @param dstSettings The daylight saving time status applicable to the time zone.
	 *
	 * @throws IllegalArgumentException if the position is not within the range 0..5.
	 */
	private fun setClock(position: Byte, timeZoneId: Short, dstSettings: DstSettings): ClocksSettings {
		val index = position.toInt().requireIn(0..5) { "TimeZones position must be in 0..5" }
		if (index >= clockCount) return this
		val clock = Clock.fromTimeZoneId(timeZoneId, dstSettings)
		return when (index) {
			0 -> this.copy(pinnedClock = clock)
			else -> this.copy(clocks = clocks.replaceAt(index - 1, clock))
		}
	}

	/**
	 * Swaps the clock at [index] with the pinned clock, the same as swapping them on the watch.
	 * Each clock moves as a whole, keeping its time zone, its type and its DST settings.
	 *
	 * @param index The index into [clocks], 0-4 for positions 1-5.
	 * @throws IllegalArgumentException if either clock is not initialized.
	 */
	fun pin(index: Int): ClocksSettings {
		val pinned = requireNotNull(pinnedClock) { "Pinned clock must be initialized" }
		val clock = requireNotNull(clocks.getOrNull(index)) { "Clock at position ${index + 1} must be initialized" }
		return copy(pinnedClock = clock, clocks = clocks.replaceAt(index, pinned))
	}

	/**
	 * Constructs a byte array representing a pair of clocks for packet transmission.
	 * E.g. Packet for home clock and world clock 1: 1D 00 01 03 02 7F 76 00 00 FF FF FF FF FF FF
	 *
	 * @param positionA The position of the first clock in the pairs.
	 * @param positionB The position of the second clock in the pairs.
	 * @param clockA The first clock, which includes its DST status and time zone.
	 * @param clockB The second clock, which includes its DST status and time zone, or null for
	 *        the unused slot after the last clock of an odd clock count. It is written as DST 00
	 *        and time zone ID FF FF, as the official app does, e.g. 1D 02 03 02 00 19 01 FF FF …
	 *
	 * @return A byte array containing a command code, positions of the clocks, DST statuses,
	 *         time zone identifiers, and padding bytes for aligning to the expected packet length.
	 */
	private fun getClocksPairPacket(
		positionA: Byte,
		positionB: Byte,
		clockA: Clock,
		clockB: Clock?,
	) = byteArrayOf(
		OpCode.CLOCK.byte,
		positionA,
		positionB,
		clockA.dstSettings.byte,
		clockB?.dstSettings?.byte ?: 0x00,
		*clockA.timeZone.identifierBytes,
		*clockB?.timeZone?.identifierBytes ?: byteArrayOf(0xFF.toByte(), 0xFF.toByte()),
	).let {
		it + ByteArray(15 - it.size) { 0xFF.toByte() }
	}

	/**
	 * Parses a packet representing two clocks and updates their configurations in the system.
	 *
	 * Validates the packet's command code and length for correctness.
	 * Extracts clock positions, DST statuses, and time zone IDs from the packet.
	 * Updates the clocks using the extracted positions and identifiers.
	 *
	 * @param clocksPacket The byte array containing the clocks packet to be parsed and processed.
	 *
	 * @throws IllegalArgumentException if the packet does not start with the expected command code
	 * or if its length is not exactly 15 bytes as required by the protocol.
	 */
	@OptIn(ExperimentalStdlibApi::class)
	fun parseClocksPacket(clocksPacket: ByteArray): ClocksSettings {
		require(clocksPacket.first() == OpCode.CLOCK.byte) {
			"Clocks packet must start with command code ${OpCode.CLOCK.byte.toHexString(HexFormat.UpperCase)}"
		}
		require(clocksPacket.size == 15) {
			"Clocks packet must be exactly 15 bytes long, e.g. 1D 00 01 03 02 7F 76 00 00 FF FF FF FF FF FF"
		}
		val positionA = clocksPacket[1]
		val positionB = clocksPacket[2]
		val dstStatusA = DstSettings.fromByte(clocksPacket[3])
		val dstStatusB = DstSettings.fromByte(clocksPacket[4])
		val timeZoneIdA = Short.fromLittleEndianByteArray(clocksPacket.sliceArray(5..6))
		val timeZoneIdB = Short.fromLittleEndianByteArray(clocksPacket.sliceArray(7..8))
		return setClock(positionA, timeZoneIdA, dstStatusA)
			.setClock(positionB, timeZoneIdB, dstStatusB)
	}

	/**
	 * Constructs and returns a list of byte arrays representing the configuration packets
	 * for all clocks currently initialized in the system.
	 *
	 * Validates that all clock objects are initialized before attempting to create packets.
	 * Generates packets as pairs: pinnedClock with clock 1, clock 2 with clock 3, and so on.
	 * With an odd clock count the last packet holds one clock and the unused slot.
	 *
	 * @return A list of clock packets represented as byte arrays, each calculated using a
	 *         helper function to encapsulate positions, DST status, and time zone IDs for
	 *         a pair of clocks.
	 *
	 * @throws IllegalArgumentException if any of the clock objects are not initialized, ensuring the
	 *         packets are only created with complete clock data.
	 */
	val clocksPackets: List<ByteArray>
		get() = allClocks
			.mapIndexed { index, clock ->
				requireNotNull(clock) {
					if (index == 0) "Pinned clock must be initialized"
					else "Clock at position $index must be initialized"
				}
			}
			.chunked(2)
			.mapIndexed { pair, clocks ->
				getClocksPairPacket(
					positionA = (pair * 2).toByte(),
					positionB = (pair * 2 + 1).toByte(),
					clockA = clocks[0],
					clockB = clocks.getOrNull(1),
				)
			}

	/**
	 * Constructs and returns a list of byte arrays representing the
	 * configuration packets for the time zones of all initialized clocks.
	 *
	 * Ensures that all clock objects are initialized prior to packet creation.
	 * Each packet consists of a command code for time zone configuration,
	 * followed by the clock's position and its time zone byte array.
	 *
	 * @return A list of byte arrays representing time zone configuration packets
	 *         for each initialized clock in the system.
	 *
	 * @throws IllegalArgumentException if any of the clock objects are not fully initialized,
	 *         ensuring integrity of time zone data in the generated packets.
	 */
	val timeZoneConfigPackets: List<ByteArray>
		get() {
			return allClocks.mapIndexed { index, clock ->
				requireNotNull(clock) { "Clock at position $index must be initialized" }
				byteArrayOf(OpCode.TIMEZONE_CONFIG.byte, index.toByte()) + clock.timeZone.bytes
			}
		}

	/**
	 * Constructs and returns a list of byte arrays representing the
	 * configuration packets for the city names of all initialized clocks.
	 *
	 * Ensures that all clock objects are initialized prior to packet creation.
	 * Each packet consists of a command code for time zone name configuration,
	 * followed by the clock's position and its city name byte array.
	 *
	 * @return A list of byte arrays representing time zone name configuration packets
	 *         for each initialized clock in the system.
	 *
	 * @throws IllegalArgumentException if any of the clock objects are not fully initialized,
	 *         ensuring integrity of city name data in the generated packets.
	 * @throws CharacterCodingException if timezone city name cannot be encoded
	 */
	val timeZoneNamePackets: List<ByteArray>
		get() {
			return allClocks.mapIndexed { index, clock ->
				requireNotNull(clock) { "Clock at position $index must be initialized" }
				byteArrayOf(
					OpCode.TIMEZONE_NAME.byte, index.toByte()
				) + clock.timeZone.cityNameBytes
			}
		}

	/**
	 * Constructs and returns a list of byte arrays representing configuration packets
	 * containing coordinates and radio IDs for all initialized clocks.
	 *
	 * Each packet includes:
	 * - A command code for the timezone location radio ID (`TIMEZONE_LOCATION_RADIO_ID`).
	 * - The clock's position identifier (ranging from 0 for pinnedClock to 5 for clock 5).
	 * - A byte of 0x01 indicates that timezone data is in the database
	 * - The timezone coordinates as a byte array.
	 * - The radio ID associated with the timezone.
	 *
	 * @return A list of byte arrays representing timezone coordinates and radio ID configuration packets.
	 *
	 * @throws IllegalArgumentException if any of the clock instances are not initialized.
	 */
	val coordinatesRadioIdPackets: List<ByteArray>
		get() {
			return allClocks.mapIndexed { index, clock ->
				requireNotNull(clock) { "Clock at position $index must be initialized" }
				byteArrayOf(
					OpCode.TIMEZONE_LOCATION_RADIO_ID.byte, index.toByte(), 1.toByte()
				) + clock.timeZone.coordinatesBytes + clock.timeZone.radioIdByte
			}
		}

	companion object {
		/**
		 * Uninitialized clocks for a watch with [clockCount] clocks, the pinned clock included.
		 */
		fun empty(clockCount: Int) = ClocksSettings(clocks = List(clockCount - 1) { null })

		/**
		 * Packet that starts a clocks update session, sent before writing clocks and timezone data.
		 * E.g. 21 00 01
		 */
		val clocksUpdateSessionStartPacket: ByteArray
			get() = byteArrayOf(
				OpCode.CLOCKS_UPDATE_SESSION.byte,
				ClocksUpdateSession.START.byte,
				0x01,
			)

		/**
		 * Packet that ends a clocks update session, sent after writing clocks and timezone data.
		 * E.g. 21 01 01
		 */
		val clocksUpdateSessionEndPacket: ByteArray
			get() = byteArrayOf(
				OpCode.CLOCKS_UPDATE_SESSION.byte,
				ClocksUpdateSession.END.byte,
				0x01,
			)
	}
}
