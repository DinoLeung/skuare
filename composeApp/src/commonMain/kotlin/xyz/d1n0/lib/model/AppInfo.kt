package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode

/**
 * App info (0x22), a token the app writes to the watch. It is how a watch is "paired", there is no
 * BLE bonding. The watch has one slot, so pairing with one app unpairs the other.
 *
 * Known layout, e.g. 22 F3 3E B0 AB 22 54 D9 DC 03 C4 02 (12 bytes):
 * [1..10] [id], per app install, every watch paired from one phone holds the same id
 * [11] [flag], 02 when written by an app, 00 on a reset watch
 *
 * Observed values:
 * new/reset watch          22 FF FF FF FF FF FF FF FF FF FF 00
 * b5600 old app version    22 2D A8 5E 24 8C 46 8C 74 83 42 02
 * b5600 newer app version  22 8C 89 73 A1 B4 16 50 2E 67 DD 02
 * 4 watches, one phone     22 F3 3E B0 AB 22 54 D9 DC 03 C4 02
 */
class AppInfo(id: ByteArray, val flag: Byte = PAIRED_FLAG) {
	init {
		require(id.size == ID_SIZE) { "App info id must be exactly $ID_SIZE bytes long, got ${id.size}" }
	}

	private val _id = id.copyOf()

	val id: ByteArray
		get() = _id.copyOf()

	/**
	 * True if the watch was reset or never paired.
	 */
	val isReset: Boolean
		get() = _id.all { it == 0xFF.toByte() }

	companion object {
		const val ID_SIZE = 10
		const val PAIRED_FLAG: Byte = 0x02

		@OptIn(ExperimentalStdlibApi::class)
		fun fromPacket(packet: ByteArray): AppInfo {
			require(packet.first() == OpCode.APP_INFO.byte) {
				"App info packet must start with command code ${
					OpCode.APP_INFO.byte.toHexString(
						HexFormat.UpperCase
					)
				}"
			}
			require(packet.size == ID_SIZE + 2) {
				"App info packet must be exactly ${ID_SIZE + 2} bytes long, e.g. 22 F3 3E B0 AB 22 54 D9 DC 03 C4 02"
			}
			return AppInfo(
				id = packet.copyOfRange(1, ID_SIZE + 1),
				flag = packet.last(),
			)
		}
	}

	val packet: ByteArray
		get() = byteArrayOf(OpCode.APP_INFO.byte, *_id, flag)

	override fun equals(other: Any?): Boolean =
		other is AppInfo && _id.contentEquals(other._id) && flag == other.flag

	override fun hashCode(): Int = 31 * _id.contentHashCode() + flag

	@OptIn(ExperimentalStdlibApi::class)
	override fun toString(): String =
		"AppInfo(id=${_id.toHexString(HexFormat.UpperCase)}, flag=${flag.toHexString(HexFormat.UpperCase)})"
}
