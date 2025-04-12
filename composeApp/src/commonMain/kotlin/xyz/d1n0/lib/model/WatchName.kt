package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode

data class WatchName(private val _name: String) {
    init {
        require(_name.length <= 19) { "Watch name length must be <= 19 characters" }
    }
    var name: String = _name
        set(value) {
            require(_name.length <= 19) { "Watch name length must be <= 19 characters" }
            field = value
        }

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun fromPacket(packet: ByteArray): WatchName {
            require(packet.first() == OpCode.WATCH_NAME.byte) {
                "Watch name packet must starts with command code ${OpCode.WATCH_NAME.byte.toHexString(HexFormat.UpperCase)}"
            }
            require(packet.size == 20) {
                "Watch name packet must be exactly 20 bytes long, e.g. 23 43 41 53 49 4F 20 47 57 2D 42 35 36 30 30 23 00 00 00 00"
            }
            return WatchName(
                packet.drop(1)
                .takeWhile { it != 0x00.toByte() }
                .toByteArray()
                .decodeToString())
        }
    }

    val packet: ByteArray
        get() = ByteArray(20) { index ->
            when (index) {
                0 -> OpCode.WATCH_SETTINGS.byte
                in 1.._name.length -> _name.get(index - 1).code.toByte()
                else -> 0x00.toByte()
            }
        }
}