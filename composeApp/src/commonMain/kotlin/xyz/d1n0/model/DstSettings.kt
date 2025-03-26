package xyz.d1n0.model

import xyz.d1n0.constant.DstBitMask

data class DstSettings(
	var enable: Boolean,
	val auto: Boolean,
) {
	companion object {
		fun fromValue(value: Int) = DstSettings(
            enable = value and DstBitMask.MASK_ENABLE != 0,
            auto = value and DstBitMask.MASK_AUTO != 0
        )
	}
	val value: Int
		get() {
			var bitMask = 0
			if (enable) bitMask = bitMask or DstBitMask.MASK_ENABLE
			if (auto) bitMask = bitMask or DstBitMask.MASK_AUTO
			return bitMask
		}
}