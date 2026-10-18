package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.DstBitMask

/**
 * The flag byte of a clock.
 *
 * @property unknownBits The bits other than [enable] and [auto], kept as read so writing the
 *           clock sends them back unchanged. The GW-BX5600 sets 0x04 on clocks 0 and 1, the
 *           official app never changes it, meaning unknown.
 */
data class DstSettings(
	val enable: Boolean,
	val auto: Boolean,
	val unknownBits: Int = 0,
) {
	companion object {
		fun fromByte(byte: Byte) = DstSettings(
			enable = byte.toInt() and DstBitMask.MASK_ENABLE != 0,
			auto = byte.toInt() and DstBitMask.MASK_AUTO != 0,
			unknownBits = byte.toInt() and 0xFF and (DstBitMask.MASK_ENABLE or DstBitMask.MASK_AUTO).inv(),
		)
	}

	val byte: Byte
		get() {
			var bitMask = unknownBits
			if (enable) bitMask = bitMask or DstBitMask.MASK_ENABLE
			if (auto) bitMask = bitMask or DstBitMask.MASK_AUTO
			return bitMask.toByte()
		}
}
