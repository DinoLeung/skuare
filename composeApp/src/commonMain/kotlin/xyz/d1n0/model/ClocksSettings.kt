package xyz.d1n0.model

import xyz.d1n0.constant.Command
import xyz.d1n0.helper.from2BytesLittleEndian

class ClocksSettings {
    lateinit var homeClock: HomeClock
    lateinit var worldClock1: WorldClock
    lateinit var worldClock2: WorldClock
    lateinit var worldClock3: WorldClock
    lateinit var worldClock4: WorldClock
    lateinit var worldClock5: WorldClock

    /**
     * Checks whether all the clock instances in the Config class are initialized.
     * @return true if homeClock and all worldClocks are initialized; false otherwise.
     */
    fun isInitialized() = ::homeClock.isInitialized
            &&::worldClock1.isInitialized
            &&::worldClock2.isInitialized
            &&::worldClock3.isInitialized
            &&::worldClock4.isInitialized
            &&::worldClock5.isInitialized

    /**
     * Sets a clock for the specified position with the given time zone ID and DST status.
     *
     * @param position The position of the clock, where 0 is homeClock and 1-5 are worldClocks.
     * @param timeZoneId The ID of the time zone to assign to the specified clock position.
     * @param dstSettings The daylight saving time status applicable to the time zone.
     *
     * @throws IllegalArgumentException if the position is not within the range 0..5.
     */
    private fun setClock(position: Int, timeZoneId: Int, dstSettings: DstSettings) =
        when (position) {
            0 -> homeClock = HomeClock.fromTimeZoneId(timeZoneId, dstSettings)
            1 -> worldClock1 = WorldClock.fromTimeZoneId(timeZoneId, dstSettings)
            2 -> worldClock2 = WorldClock.fromTimeZoneId(timeZoneId, dstSettings)
            3 -> worldClock3 = WorldClock.fromTimeZoneId(timeZoneId, dstSettings)
            4 -> worldClock4 = WorldClock.fromTimeZoneId(timeZoneId, dstSettings)
            5 -> worldClock5 = WorldClock.fromTimeZoneId(timeZoneId, dstSettings)
            else -> require(position in 0..5) { "TimeZones position must be in 0..5" }
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
    private fun getClocksPairPacket(positionA: Int, positionB: Int, clockA: Clock, clockB: Clock) =
        byteArrayOf(
            Command.CLOCK.value.toByte(),
            positionA.toByte(),
            positionB.toByte(),
            clockA.dstSettings.value.toByte(),
            clockB.dstSettings.value.toByte(),
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
        require(clocksPacket.first() == Command.CLOCK.value.toByte()) {
            "Clocks packet must starts with command code ${Command.CLOCK.value.toHexString(HexFormat.UpperCase)}"
        }
        require(clocksPacket.size == 15) {
            "Clocks packet must be exactly 15 bytes long, e.g. 1D 00 01 03 02 7F 76 00 00 FF FF FF FF FF FF"
        }
        runCatching {
            val positionA = clocksPacket[1].toInt()
            val positionB = clocksPacket[2].toInt()
            val dstStatusA = DstSettings.fromValue(clocksPacket[3].toInt())
            val dstStatusB = DstSettings.fromValue(clocksPacket[4].toInt())
            val timeZoneIdA = Int.from2BytesLittleEndian(clocksPacket.sliceArray(5..6))
            val timeZoneIdB = Int.from2BytesLittleEndian(clocksPacket.sliceArray(7..8))

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
            require(isInitialized()) { "Clocks must be initialized" }
            return listOf(
                getClocksPairPacket(0, 1, homeClock, worldClock1),
                getClocksPairPacket(2, 3, worldClock2, worldClock3),
                getClocksPairPacket(4, 5, worldClock4, worldClock5)
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
            require(isInitialized()) { "Clocks must be initialized" }
            return listOf(
                byteArrayOf(Command.TIMEZONE_CONFIG.value.toByte(), 0.toByte()) + homeClock.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_CONFIG.value.toByte(), 1.toByte()) + worldClock1.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_CONFIG.value.toByte(), 2.toByte()) + worldClock2.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_CONFIG.value.toByte(), 3.toByte()) + worldClock3.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_CONFIG.value.toByte(), 4.toByte()) + worldClock4.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_CONFIG.value.toByte(), 5.toByte()) + worldClock5.timeZone.bytes,
            )
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
            require(isInitialized()) { "Clocks must be initialized" }
            return listOf(
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 0.toByte()) + homeClock.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 1.toByte()) + worldClock1.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 2.toByte()) + worldClock2.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 3.toByte()) + worldClock3.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 4.toByte()) + worldClock4.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 5.toByte()) + worldClock5.timeZone.cityNameBytes,
            )
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
            require(isInitialized()) { "Clocks must be initialized" }
            return listOf(
                byteArrayOf(Command.TIMEZONE_LOCATION_RADIO_ID.value.toByte(), 0.toByte(), 1.toByte()) + homeClock.timeZone.coordinatesBytes + homeClock.timeZone.radioIdByte,
                byteArrayOf(Command.TIMEZONE_LOCATION_RADIO_ID.value.toByte(), 1.toByte(), 1.toByte()) + worldClock1.timeZone.coordinatesBytes + worldClock1.timeZone.radioIdByte,
                byteArrayOf(Command.TIMEZONE_LOCATION_RADIO_ID.value.toByte(), 2.toByte(), 1.toByte()) + worldClock2.timeZone.coordinatesBytes + worldClock2.timeZone.radioIdByte,
                byteArrayOf(Command.TIMEZONE_LOCATION_RADIO_ID.value.toByte(), 3.toByte(), 1.toByte()) + worldClock3.timeZone.coordinatesBytes + worldClock3.timeZone.radioIdByte,
                byteArrayOf(Command.TIMEZONE_LOCATION_RADIO_ID.value.toByte(), 4.toByte(), 1.toByte()) + worldClock4.timeZone.coordinatesBytes + worldClock4.timeZone.radioIdByte,
                byteArrayOf(Command.TIMEZONE_LOCATION_RADIO_ID.value.toByte(), 5.toByte(), 1.toByte()) + worldClock5.timeZone.coordinatesBytes + worldClock5.timeZone.radioIdByte,
            )
        }
}
