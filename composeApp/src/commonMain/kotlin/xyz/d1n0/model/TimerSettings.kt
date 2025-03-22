package xyz.d1n0.model

class TimerSettings {
    lateinit var timer: Timer

    fun parseTimerPacket(packet: ByteArray) {
        timer = Timer.fromPacket(packet)
    }

    val timerPacket: ByteArray
        get() {
            require(::timer.isInitialized) { "Timer must be initialized" }
            return timer.packet
        }
}
