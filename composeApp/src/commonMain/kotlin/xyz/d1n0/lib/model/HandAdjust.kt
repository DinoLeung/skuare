package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.HandAction
import xyz.d1n0.lib.constant.HandAdjustSession
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.lib.constant.OpCode
import kotlin.math.absoluteValue

/**
 * Hand adjustment (home position correction) packets, opcode [OpCode.HAND_ADJUST] written to the
 * IO characteristic. Decoded from the official app on a GA-B2100, see
 * research/hand-adjust-research.md.
 *
 * Every packet is `1A 04 <command> <steps LE16> 00`. The command is a [HandAdjustSession], or a
 * [HandAction] on a [HandGroup]. The watch's only reply is [ACK], sent when it has finished a
 * command that moves hands, and for [HandAdjustSession.OPEN].
 */
object HandAdjust {
	/** Probably the payload length, always 04. */
	private const val LENGTH: Byte = 0x04

	/** The watch's reply, e.g. when moving hands has finished. */
	val ACK: ByteArray get() = byteArrayOf(OpCode.HAND_ADJUST.byte, LENGTH, 0, 0, 0, 0)

	val openPacket: ByteArray get() = packet(HandAdjustSession.OPEN.byte)
	val savePacket: ByteArray get() = packet(HandAdjustSession.SAVE.byte)
	val closePacket: ByteArray get() = packet(HandAdjustSession.CLOSE.byte)

	fun selectPacket(group: HandGroup): ByteArray = packet(HandAction.SELECT.commandFor(group))

	/** Moves [group]'s hands to the adjusting position, a move of 0 steps. */
	fun adjustPositionPacket(group: HandGroup): ByteArray = packet(HandAction.MOVE.commandFor(group))

	/**
	 * Moves [group]'s hands [steps] from where they are, clockwise if positive. Anticlockwise is
	 * sent as [HandGroup.stepsPerRevolution] − n, e.g. −2 on the clock hands is 2158.
	 *
	 * @throws IllegalArgumentException if [steps] is 0 or a whole revolution or more, both of which
	 *         would be sent as 0, the move to the adjusting position.
	 */
	fun movePacket(group: HandGroup, steps: Int): ByteArray {
		require(steps != 0 && steps.absoluteValue < group.stepsPerRevolution) {
			"Steps must be non-zero and less than a revolution (${group.stepsPerRevolution}), got $steps"
		}
		return packet(HandAction.MOVE.commandFor(group), steps.mod(group.stepsPerRevolution))
	}

	/** Sent right after each move's [ACK], maybe confirms it. */
	fun confirmPacket(group: HandGroup): ByteArray = packet(HandAction.CONFIRM.commandFor(group))

	private fun packet(command: Byte, steps: Int = 0) = byteArrayOf(
		OpCode.HAND_ADJUST.byte,
		LENGTH,
		command,
		(steps and 0xFF).toByte(),
		(steps shr 8 and 0xFF).toByte(),
		0x00,
	)

	/** Whether [packet] is a hand adjust reply, or an error the watch sent for one. */
	internal fun isReply(packet: ByteArray) = when (packet.firstOrNull()) {
		OpCode.HAND_ADJUST.byte -> true
		OpCode.ERROR.byte -> packet.getOrNull(2) == OpCode.HAND_ADJUST.byte
		else -> false
	}
}
