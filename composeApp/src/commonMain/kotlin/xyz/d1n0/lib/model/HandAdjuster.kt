package xyz.d1n0.lib.model

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.lib.constant.OpCode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs hand adjustment the way the official app does on the GA-B2100:
 * [start], then [select] a group, [move] it as often as needed, and [finish]. See
 * research/hand-adjust-research.md for the captured sequence and its open questions.
 *
 * @param write Writes a packet to the IO characteristic, see [Watch.write].
 * @param packets Packets received on the IO characteristic, see [Watch.packets].
 * @param ackTimeout How long to wait for the watch to reply to [HandAdjust.openPacket].
 * @param moveTimeout How long to wait for hands to stop. The GA-B2100's hour and minute hands took
 *        10.7 s to reach the adjusting position.
 */
class HandAdjuster(
	private val write: suspend (ByteArray) -> Unit,
	private val packets: Flow<ByteArray>,
	private val ackTimeout: Duration = 5.seconds,
	private val moveTimeout: Duration = 30.seconds,
) {
	/** Enters adjust mode, the official app sends it when the selection screen opens. */
	suspend fun start() = writeAndAwaitAck(HandAdjust.openPacket, ackTimeout)

	/** Selects [group] and returns once its hands are at the adjusting position. */
	suspend fun select(group: HandGroup) {
		write(HandAdjust.selectPacket(group))
		writeAndAwaitAck(HandAdjust.adjustPositionPacket(group), moveTimeout)
	}

	/** Moves [group]'s hands [steps], see [HandAdjust.movePacket], and returns once they stop. */
	suspend fun move(group: HandGroup, steps: Int) {
		writeAndAwaitAck(HandAdjust.movePacket(group, steps), moveTimeout)
		write(HandAdjust.confirmPacket(group))
	}

	/**
	 * Leaves adjust mode, the hands go back to the time. The watch sends nothing when they get
	 * there.
	 */
	suspend fun finish() {
		write(HandAdjust.savePacket)
		writeAndAwaitAck(HandAdjust.openPacket, ackTimeout)
		write(HandAdjust.closePacket)
	}

	/**
	 * Writes [packet] and waits for the watch's reply.
	 *
	 * @throws WatchErrorPacket if the watch replies with an error.
	 * @throws IllegalStateException if it doesn't reply within [timeout].
	 */
	@OptIn(ExperimentalStdlibApi::class)
	private suspend fun writeAndAwaitAck(packet: ByteArray, timeout: Duration) {
		val reply = withTimeoutOrNull(timeout) {
			coroutineScope {
				// subscribe before writing, so no reply arrives before we listen
				val reply = async(start = CoroutineStart.UNDISPATCHED) { packets.first(HandAdjust::isReply) }
				write(packet)
				reply.await()
			}
		}
		checkNotNull(reply) {
			"No reply to hand adjust ${packet.toHexString(HexFormat.UpperCase)} within $timeout"
		}
		if (reply.first() == OpCode.ERROR.byte) throw WatchErrorPacket(reply)
	}
}
