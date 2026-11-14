package xyz.d1n0.lib.constant

/**
 * Hands that are adjusted together, see [xyz.d1n0.lib.model.HandAdjust]. The values are the
 * GA-B2100's, the only analog watch captured so far (research/hand-adjust-research.md). If a
 * later model has other groups or sizes, they move into its profile.
 *
 * @property id High nibble of the hand adjust command byte.
 * @property stepsPerRevolution Where the relative step count wraps, so −n is sent as
 *           [stepsPerRevolution] − n.
 */
enum class HandGroup(val id: Int, val stepsPerRevolution: Int) {
	/** Hour and minute hands. One step is one minute, 2160 is 36 hours (3 hour-hand turns). */
	CLOCK(id = 1, stepsPerRevolution = 2160),

	/** The function hand, 2° per step if 180 is one turn (unconfirmed). */
	FUNCTION(id = 2, stepsPerRevolution = 180);

	override fun toString(): String = when (this) {
		CLOCK -> "Hour and minute hands"
		FUNCTION -> "Function hand"
	}
}
