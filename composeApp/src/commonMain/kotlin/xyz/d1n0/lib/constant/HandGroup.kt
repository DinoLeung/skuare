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

	/**
	 * The mode hand, 1 step is 2°, so 180 is a full turn. Its dial is a half circle, but it can be
	 * adjusted all the way round. The adjusting position is 3 o'clock, 9 o'clock is 90 steps away.
	 */
	MODE(id = 2, stepModulus = 180);

	override fun toString(): String = when (this) {
		CLOCK -> "Clock hands"
		MODE -> "Mode hand"
	}
}
