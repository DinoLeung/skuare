package xyz.d1n0.lib.constant

import kotlin.jvm.JvmInline

@JvmInline
value class AutoSyncDelay(val minutes: Int) {
	init {
		require(minutes in 0..59) {
			"AutoSyncDelay must be between 0 and 59, got $minutes"
		}
	}

	val byte: Byte
		get() = minutes.toByte()

	companion object {
		@OptIn(ExperimentalStdlibApi::class)
		fun fromByte(byte: Byte): AutoSyncDelay =
			AutoSyncDelay(byte.toInt())
	}

	override fun toString(): String {
		return minutes.toString()
	}
}

//enum class AutoSyncDelay(val byte: Byte) {
//	MINUTE_0(0),
//	MINUTE_1(1),
//	MINUTE_2(2),
//	MINUTE_3(3),
//	MINUTE_4(4),
//	MINUTE_5(5),
//	MINUTE_6(6),
//	MINUTE_7(7),
//	MINUTE_8(8),
//	MINUTE_9(9),
//	MINUTE_10(10),
//	MINUTE_11(11),
//	MINUTE_12(12),
//	MINUTE_13(13),
//	MINUTE_14(14),
//	MINUTE_15(15),
//	MINUTE_16(16),
//	MINUTE_17(17),
//	MINUTE_18(18),
//	MINUTE_19(19),
//	MINUTE_20(20),
//	MINUTE_21(21),
//	MINUTE_22(22),
//	MINUTE_23(23),
//	MINUTE_24(24),
//	MINUTE_25(25),
//	MINUTE_26(26),
//	MINUTE_27(27),
//	MINUTE_28(28),
//	MINUTE_29(29),
//	MINUTE_30(30),
//	MINUTE_31(31),
//	MINUTE_32(32),
//	MINUTE_33(33),
//	MINUTE_34(34),
//	MINUTE_35(35),
//	MINUTE_36(36),
//	MINUTE_37(37),
//	MINUTE_38(38),
//	MINUTE_39(39),
//	MINUTE_40(40),
//	MINUTE_41(41),
//	MINUTE_42(42),
//	MINUTE_43(43),
//	MINUTE_44(44),
//	MINUTE_45(45),
//	MINUTE_46(46),
//	MINUTE_47(47),
//	MINUTE_48(48),
//	MINUTE_49(49),
//	MINUTE_50(50),
//	MINUTE_51(51),
//	MINUTE_52(52),
//	MINUTE_53(53),
//	MINUTE_54(54),
//	MINUTE_55(55),
//	MINUTE_56(56),
//	MINUTE_57(57),
//	MINUTE_58(58),
//	MINUTE_59(59);
//
//	companion object {
//		@OptIn(ExperimentalStdlibApi::class)
//		fun fromByte(byte: Byte) =
//			AutoSyncDelay.entries.firstOrNull { it.byte == byte }
//				?: error("Unknown auto sync delay value: ${byte.toHexString(HexFormat.UpperCase)}")
//	}
//}