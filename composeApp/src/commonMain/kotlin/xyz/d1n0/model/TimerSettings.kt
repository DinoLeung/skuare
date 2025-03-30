package xyz.d1n0.model

import xyz.d1n0.constant.Command

class TimerSettings {
    lateinit var timer: Timer

    @OptIn(ExperimentalStdlibApi::class)
    fun parseTimerPacket(packet: ByteArray) {
        require(packet.first() == Command.TIMER.byte) {
            "Timer packet must starts with command code ${Command.TIMER.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 8) {
            "Timer packet bytes must be exactly 8 bytes long, e.g. 18 17 0F 1E 00 00 00 00"
        }
        timer = Timer.fromBytes(packet.sliceArray(1..packet.lastIndex))
    }

    val timerPacket: ByteArray
        get() {
            require(::timer.isInitialized) { "Timer must be initialized" }
            return byteArrayOf(Command.TIMER.byte, *timer.bytes)
        }
}
