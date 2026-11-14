package xyz.d1n0.lib.constant

/**
 * Hands that are adjusted together, see [xyz.d1n0.lib.model.HandAdjust]. The values are the
 * GA-B2100's, the only analog watch captured so far (research/hand-adjust-research.md). If a
 * later model has other groups or moduli, they move into its profile.
 *
 * @property id High nibble of the hand adjust command byte, see [HandAction.commandFor].
 * @property stepModulus Moves are sent modulo this, so an anticlockwise move of n steps is sent as
 *           [stepModulus] − n. Not necessarily one turn of a hand: the clock hands' 2160 is 36 hours.
 */
enum class HandGroup(val id: Int, val stepModulus: Int) {
	/** Hour and minute hands. One step is one minute, 2160 is 36 hours (3 hour-hand turns). */
	CLOCK(id = 1, stepModulus = 2160),

	/** The function hand. 2° per step if 180 is one turn (unconfirmed). */
	FUNCTION(id = 2, stepModulus = 180);

	override fun toString(): String = when (this) {
		CLOCK -> "Hour and minute hands"
		FUNCTION -> "Function hand"
	}
}
