package xyz.d1n0.lib.constant

/**
 * Font style, the 0x20 bit of watch settings byte 8. Only MIP square watches have it, see
 * [xyz.d1n0.lib.model.WatchModel.hasFontStyle].
 */
enum class FontStyle(val byte: Byte) {
	STANDARD(0x00),
	CLASSIC(0x20);

	companion object {
		private const val CLASSIC_BIT = 0x20

		// Other bits of the byte are not known, so only the font bit is read
		fun fromByte(byte: Byte) =
			if (byte.toInt() and CLASSIC_BIT != 0) CLASSIC else STANDARD
	}

	override fun toString(): String = when (this) {
		STANDARD -> "Standard"
		CLASSIC -> "Classic"
	}
}
