package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.DstBitMask

data class DstSettings(
	val enable: Boolean,
	val auto: Boolean,
) {
	companion object {
		fun fromByte(byte: Byte) = DstSettings(
			enable = byte.toInt() and DstBitMask.MASK_ENABLE != 0,
			auto = byte.toInt() and DstBitMask.MASK_AUTO != 0
		)
	}

	val byte: Byte
		get() {
			var bitMask = 0
			if (enable) bitMask = bitMask or DstBitMask.MASK_ENABLE
			if (auto) bitMask = bitMask or DstBitMask.MASK_AUTO
			return bitMask.toByte()
		}
}