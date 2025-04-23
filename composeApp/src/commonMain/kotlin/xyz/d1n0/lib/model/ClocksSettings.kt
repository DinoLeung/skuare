package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode
import xyz.d1n0.lib.helper.fromLittleEndianByteArray
import xyz.d1n0.lib.helper.requireIn

data class ClocksSettings(
	var homeClock: HomeClock? = null,
	var worldClocks: List<WorldClock?> = List(5) { null },
) {
	private val allClocks: List<Clock?>
		get() = listOf(
			homeClock,
			*worldClocks.toTypedArray(),
		)

	val isInitialized: Boolean get() = listOfNotNull(*allClocks.toTypedArray()).size == 6

	/**
	 * Sets a clock for the specified position with the given time zone ID and DST status.
	 *
	 * @param position The position of the clock, where 0 is homeClock and 1-5 are worldClocks.
	 * @param timeZoneId The ID of the time zone to assign to the specified clock position.
	 * @param dstSettings The daylight saving time status applicable to the time zone.
	 *
	 * @throws IllegalArgumentException if the position is not within the range 0..5.
	 */
	private fun setClock(position: Byte, timeZoneId: Short, dstSettings: DstSettings) =
		when (position.toInt()) {
			0 -> {
				homeClock = HomeClock.fromTimeZoneId(timeZoneId, dstSettings)
			}

			else -> {
				val index =
					position.toInt().requireIn(1..5) { "TimeZones position must be in 0..5" }
						.minus(1)
				val clock = WorldClock.fromTimeZoneId(timeZoneId, dstSettings)
				worldClocks = worldClocks.toMutableList().also { it[index] = clock }
			}
		}

	/**
	 * Constructs a byte array representing a pair of clocks for packet transmission.
	 * E.g. Packet for home clock and world clock 1: 1D 00 01 03 02 7F 76 00 00 FF FF FF FF FF FF
	 *
	 * @param positionA The position of the first clock in the pairs.
	 * @param positionB The position of the second clock in the pairs.
	 * @param clockA The first clock, which includes its DST status and time zone.
	 * @param clockB The second clock, which includes its DST status and time zone.
	 *
	 * @return A byte array containing a command code, positions of the clocks, DST statuses,
	 *         time zone identifiers, and padding bytes for aligning to the expected packet length.
	 */
	private fun getClocksPairPacket(
		positionA: Byte,
		positionB: Byte,
		clockA: Clock,
		clockB: Clock,
	) = byteArrayOf(
		OpCode.CLOCK.byte,
		positionA,
		positionB,
		clockA.dstSettings.byte,
		clockB.dstSettings.byte,
		*clockA.timeZone.identifierBytes,
		*clockB.timeZone.identifierBytes,
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
	fun parseClocksPacket(clocksPacket: ByteArray) {
		require(clocksPacket.first() == OpCode.CLOCK.byte) {
			"Clocks packet must starts with command code ${OpCode.CLOCK.byte.toHexString(HexFormat.UpperCase)}"
		}
		require(clocksPacket.size == 15) {
			"Clocks packet must be exactly 15 bytes long, e.g. 1D 00 01 03 02 7F 76 00 00 FF FF FF FF FF FF"
		}
		runCatching {
			val positionA = clocksPacket[1]
			val positionB = clocksPacket[2]
			val dstStatusA = DstSettings.fromByte(clocksPacket[3])
			val dstStatusB = DstSettings.fromByte(clocksPacket[4])
			val timeZoneIdA = Short.fromLittleEndianByteArray(clocksPacket.sliceArray(5..6))
			val timeZoneIdB = Short.fromLittleEndianByteArray(clocksPacket.sliceArray(7..8))

			setClock(positionA, timeZoneIdA, dstStatusA)
			setClock(positionB, timeZoneIdB, dstStatusB)
		}.onFailure {
			throw IllegalArgumentException("Invalid clocks packet", it)
		}
	}

	/**
	 * Constructs and returns a list of byte arrays representing the configuration packets
	 * for all clocks currently initialized in the system.
	 *
	 * Validates that all clock objects are initialized before attempting to create packets.
	 * Generates packets as pairs: homeClock with worldClock1, worldClock2 with worldClock3,
	 * and worldClock4 with worldClock5.
	 *
	 * @return A list of clock packets represented as byte arrays, each calculated using a
	 *         helper function to encapsulate positions, DST status, and time zone IDs for
	 *         a pair of clocks.
	 *
	 * @throws IllegalArgumentException if any of the clock objects are not initialized, ensuring the
	 *         packets are only created with complete clock data.
	 */
	val clocksPackets: List<ByteArray>
		get() {
			val homeClockValue = requireNotNull(homeClock) { "HomeClock must be initialized" }
			val worldClockValues = worldClocks.mapIndexed { index, clock ->
				requireNotNull(clock) { "WorldClock at position $index must be initialized" }
			}
			return listOf(
				getClocksPairPacket(0, 1, homeClockValue, worldClockValues[0]),
				getClocksPairPacket(2, 3, worldClockValues[1], worldClockValues[2]),
				getClocksPairPacket(4, 5, worldClockValues[3], worldClockValues[4])
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
	 * - The clock's position identifier (ranging from 0 for homeClock to 5 for worldClock5).
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
}
