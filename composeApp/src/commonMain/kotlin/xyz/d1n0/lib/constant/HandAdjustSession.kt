package xyz.d1n0.lib.constant

/**
 * Hand adjust commands for the whole session, the full command byte of a
 * [xyz.d1n0.lib.model.HandAdjust] packet. Per-group commands combine a [HandGroup] with a
 * [HandAction] instead. Names are inferred from the official app, the real ones are unknown.
 */
enum class HandAdjustSession(val byte: Byte) {
	/** Sent when the selection screen opens, and again on exit. The watch replies to it. */
	OPEN(0x06),

	/** First write on exit, maybe saves the new position. */
	SAVE(0x04),

	/** Last write on exit, leaves adjust mode, the hands go back to the time. */
	CLOSE(0x07);
}
