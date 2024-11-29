package xyz.d1n0.model

import xyz.d1n0.helper.BytesConverter

class Config {
    lateinit var homeClock: HomeClock
    lateinit var worldClock1: WorldClock
    lateinit var worldClock2: WorldClock
    lateinit var worldClock3: WorldClock
    lateinit var worldClock4: WorldClock
    lateinit var worldClock5: WorldClock

    private fun areClocksInitialized() =
        ::homeClock.isInitialized && ::worldClock1.isInitialized && ::worldClock2.isInitialized && ::worldClock3.isInitialized && ::worldClock4.isInitialized && ::worldClock5.isInitialized

    private fun setClock(position: Int, timeZoneId: Int, dstStatus: DstStatus) {
        when (position) {
            0 -> homeClock = HomeClock.fromTimeZoneId(timeZoneId, dstStatus)
            1 -> worldClock1 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            2 -> worldClock2 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            3 -> worldClock3 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            4 -> worldClock4 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            5 -> worldClock5 = WorldClock.fromTimeZoneId(timeZoneId, dstStatus)
            else -> throw ConfigError("TimeZones position must be in 0..5")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseClocks(clocksPacket: ByteArray) {
        if (clocksPacket.first() != Command.clock.code.toByte()) {
            throw ConfigError("Clocks packet must starts with command code ${Command.clock.code.toHexString(HexFormat.UpperCase)}")
        }
        if (clocksPacket.size != 15) {
            throw ConfigError("Clocks packet must be exactly 15 bytes long, e.g. 1D 00 01 03 02 7F 76 00 00 FF FF FF FF FF FF")
        }

        val position1 = clocksPacket[1].toInt()
        val position2 = clocksPacket[2].toInt()
        val dstStatuc1 = DstStatus.fromValue(clocksPacket[3].toInt())
        val dstStatuc2 = DstStatus.fromValue(clocksPacket[4].toInt())
        val timeZoneId1 = BytesConverter.littleEdianBytesToInt(clocksPacket.sliceArray(5..6))
        val timeZoneId2 = BytesConverter.littleEdianBytesToInt(clocksPacket.sliceArray(7..8))

        setClock(position1, timeZoneId1, dstStatuc1)
        setClock(position2, timeZoneId2, dstStatuc2)
    }

    fun getClocksPackets(): List<ByteArray> {
        if (!areClocksInitialized()) {
            throw ConfigError("Clocks must be initialized")
        }
        TODO("implement clock pair packet")
    }
}

class ConfigError(message: String) : Exception(message)
