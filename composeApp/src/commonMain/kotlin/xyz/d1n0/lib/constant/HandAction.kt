package xyz.d1n0.lib.constant

/**
 * What a per-group hand adjust command does, the low nibble of its command byte. The high nibble is
 * the [HandGroup.id], e.g. [MOVE] on [HandGroup.MODE] is 0x28. Commands for the whole session
 * are [HandAdjustSession]. Names are inferred from the official app, the real ones are unknown.
 */
enum class HandAction(val nibble: Int) {
	/** Selects the group, the watch doesn't reply. */
	SELECT(0x2),

	/**
	 * Moves the group's hands, the watch replies once they stop. 0 steps moves them to the
	 * adjusting position, see [xyz.d1n0.lib.model.HandAdjust.movePacket] for other moves.
	 */
	MOVE(0x8),

	/** Sent right after each move's reply, maybe confirms it. The watch doesn't reply. */
	CONFIRM(0x9);

	/** The command byte for this action on [group]. */
	fun commandFor(group: HandGroup): Byte = ((group.id shl 4) or nibble).toByte()
}
