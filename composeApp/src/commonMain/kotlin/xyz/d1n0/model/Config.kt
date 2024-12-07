package xyz.d1n0.model

import xyz.d1n0.constant.Command
import xyz.d1n0.constant.DstStatus
import xyz.d1n0.helper.from2BytesLittleEndian

class Config {
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
    private fun areClocksInitialized() =
        ::homeClock.isInitialized && ::worldClock1.isInitialized && ::worldClock2.isInitialized && ::worldClock3.isInitialized && ::worldClock4.isInitialized && ::worldClock5.isInitialized

    /**
     * Sets a clock for the specified position with the given time zone ID and DST status.
     *
     * @param position The position of the clock, where 0 is homeClock and 1-5 are worldClocks.
     * @param timeZoneId The ID of the time zone to assign to the specified clock position.
     * @param dstStatus The daylight saving time status applicable to the time zone.
     *
     * @throws IllegalArgumentException if the position is not within the range 0..5.
     */
    private fun setClock(position: Int, timeZoneId: Int, dstStatus: DstStatus) =
        when (position) {
            0 -> homeClock = HomeClock.fromTimeZoneId(timeZoneId, dstStatus)
            1 -> worldClock1 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            2 -> worldClock2 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            3 -> worldClock3 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            4 -> worldClock4 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            5 -> worldClock5 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
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
            clockA.dstStatus.value.toByte(),
            clockB.dstStatus.value.toByte(),
            *clockA.timeZone.identifierBytes,
            *clockB.timeZone.identifierBytes,
        ) + ByteArray(5) { 0xFF.toByte() }

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
            val dstStatusA = DstStatus.fromValue(clocksPacket[3].toInt())
            val dstStatusB = DstStatus.fromValue(clocksPacket[4].toInt())
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
            require(areClocksInitialized()) { "Clocks must be initialized" }
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
            require(areClocksInitialized()) { "Clocks must be initialized" }
            return listOf(
                byteArrayOf(Command.TIMEZONE_INFO.value.toByte(), 0.toByte()) + homeClock.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_INFO.value.toByte(), 1.toByte()) + worldClock1.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_INFO.value.toByte(), 2.toByte()) + worldClock2.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_INFO.value.toByte(), 3.toByte()) + worldClock3.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_INFO.value.toByte(), 4.toByte()) + worldClock4.timeZone.bytes,
                byteArrayOf(Command.TIMEZONE_INFO.value.toByte(), 5.toByte()) + worldClock5.timeZone.bytes,
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
            require(areClocksInitialized()) { "Clocks must be initialized" }
            return listOf(
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 0.toByte()) + homeClock.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 1.toByte()) + worldClock1.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 2.toByte()) + worldClock2.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 3.toByte()) + worldClock3.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 4.toByte()) + worldClock4.timeZone.cityNameBytes,
                byteArrayOf(Command.TIMEZONE_NAME.value.toByte(), 5.toByte()) + worldClock5.timeZone.cityNameBytes,
            )
        }
}
