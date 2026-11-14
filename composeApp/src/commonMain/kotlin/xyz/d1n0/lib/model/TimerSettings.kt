package xyz.d1n0.lib.model

/**
 * @property timer The timer the watch last sent, null until requested.
 * @property packetSize Size of the watch's last timer reply, see [Timer.packet]. Writes use the
 *           same size, as the official app does.
 */
data class TimerSettings(
	val timer: Timer? = null,
	val packetSize: Int = Timer.CLASSIC_PACKET_SIZE,
) {
	fun parseTimerPacket(packet: ByteArray) =
		this.copy(timer = Timer.fromPacket(packet), packetSize = packet.size)
}
