package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.d1n0.lib.constant.OpCode

class TimerSettings {
    lateinit var timer: Timer

    val isInitialized: StateFlow<Boolean> get() = _isInitializedFlow
    private val _isInitializedFlow = MutableStateFlow(false)
    private fun updateInitializedState() {
        _isInitializedFlow.value = ::timer.isInitialized
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parseTimerPacket(packet: ByteArray) {
        require(packet.first() == OpCode.TIMER.byte) {
            "Timer packet must starts with command code ${OpCode.TIMER.byte.toHexString(HexFormat.UpperCase)}"
        }
        require(packet.size == 8) {
            "Timer packet bytes must be exactly 8 bytes long, e.g. 18 17 0F 1E 00 00 00 00"
        }
        timer = Timer.fromBytes(packet.sliceArray(1..packet.lastIndex))
        updateInitializedState()
    }

    val timerPacket: ByteArray
        get() {
            require(::timer.isInitialized) { "Timer must be initialized" }
            return byteArrayOf(OpCode.TIMER.byte, *timer.bytes)
        }
}
