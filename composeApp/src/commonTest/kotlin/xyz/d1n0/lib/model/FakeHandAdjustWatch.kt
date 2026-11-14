package xyz.d1n0.lib.model

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Records hand adjust writes and replies like the GA-B2100 by default: an ack for open and for
 * every move. [reply] returns the packet to send back for a write, null for none.
 */
@OptIn(ExperimentalStdlibApi::class)
internal class FakeHandAdjustWatch(private val reply: (ByteArray) -> ByteArray? = ::ackLikeGaB2100) {
	val packets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
	val written = mutableListOf<String>()

	suspend fun write(packet: ByteArray) {
		written += packet.toHexString()
		reply(packet)?.let { packets.emit(it) }
	}

	fun adjuster() = HandAdjuster(write = { write(it) }, packets = packets)

	companion object {
		fun ackLikeGaB2100(packet: ByteArray): ByteArray? {
			val command = packet[2].toInt()
			return if (command == 0x06 || command and 0x0F == 0x08) HandAdjust.ACK else null
		}
	}
}
