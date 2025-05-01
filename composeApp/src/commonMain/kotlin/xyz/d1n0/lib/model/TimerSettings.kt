package xyz.d1n0.lib.model

data class TimerSettings(
	val timer: Timer? = null,
) {
	@OptIn(ExperimentalStdlibApi::class)
	fun parseTimerPacket(packet: ByteArray) = this.copy(timer = Timer.fromPacket(packet))
}
