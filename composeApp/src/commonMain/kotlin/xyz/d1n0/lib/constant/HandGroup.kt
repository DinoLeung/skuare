package xyz.d1n0.lib.constant

/**
 * Hands that are adjusted together, see [xyz.d1n0.lib.model.HandAdjust]. The values are the
 * GA-B2100's, the only analog watch captured so far (research/hand-adjust-research.md). If a
 * later model has other groups or moduli, they move into its profile.
 *
 * @property id High nibble of the hand adjust command byte, see [HandAction.commandFor].
 * @property stepModulus Moves are sent modulo this, so an anticlockwise move of n steps is sent as
 *           [stepModulus] − n.
 */
enum class HandGroup(val id: Int, val stepModulus: Int) {
	/**
	 * Clock hands, adjusted by the minute hand. 1 step is 2° of the minute hand (20 seconds), 15
	 * steps from 12 to 1. 2160 is 12 hours, one turn of the hour hand.
	 */
	CLOCK(id = 1, stepModulus = 2160),

	/** The function hand, its dial is a half circle, 1 step is 1°. */
	FUNCTION(id = 2, stepModulus = 180);

	override fun toString(): String = when (this) {
		CLOCK -> "Hour and minute hands"
		FUNCTION -> "Function hand"
	}
}
