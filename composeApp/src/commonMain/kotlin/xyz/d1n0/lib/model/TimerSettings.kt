package xyz.d1n0.lib.model

class TimerSettings {
    var timer: Timer? = null

    val isInitialized: Boolean get() = timer != null

    @OptIn(ExperimentalStdlibApi::class)
    fun parseTimerPacket(packet: ByteArray) {
        timer = Timer.fromPacket(packet)
    }
}
