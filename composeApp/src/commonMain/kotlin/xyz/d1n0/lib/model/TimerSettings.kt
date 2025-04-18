package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.d1n0.lib.constant.OpCode

class TimerSettings {
    private val _timer = MutableStateFlow<Timer?>(null)
    val timer: StateFlow<Timer?> get() = _timer.asStateFlow()

    val isInitialized: StateFlow<Boolean> get() = _isInitialized.asStateFlow()
    private val _isInitialized = MutableStateFlow(false)
    private fun updateInitialized() =
        _isInitialized.update { _timer.value != null }


    @OptIn(ExperimentalStdlibApi::class)
    fun parseTimerPacket(packet: ByteArray) {
        require(packet.first() == OpCode.TIMER.byte) {
            "Timer packet must starts with command code ${OpCode.TIMER.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 8) {
            "Timer packet bytes must be exactly 8 bytes long, e.g. 18 17 0F 1E 00 00 00 00"
        }
        _timer.update { Timer.fromBytes(packet.sliceArray(1..packet.lastIndex)) }
            .also { updateInitialized() }
    }

    val timerPacket: ByteArray
        get() {
            val timerValue = requireNotNull(_timer.value) { "Timer must be initialized" }
            return byteArrayOf(OpCode.TIMER.byte, *timerValue.bytes)
        }
}
