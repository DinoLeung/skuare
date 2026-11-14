package xyz.d1n0.lib.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import xyz.d1n0.lib.constant.BatteryLevel
import xyz.d1n0.lib.constant.WatchStatusSession
import xyz.d1n0.lib.helper.fromBcdByte
import xyz.d1n0.lib.helper.fromBcdByteArray

/**
 * Watch status (battery level and more), sent on the data transfer characteristic after
 * [requestPacket] is written to the data request characteristic. The official app requests it
 * on every connect.
 *
 * Each watch replies with its own fixed size, whatever size is requested, so the layout is picked
 * from the reply's size, see [Layout]. Layouts in research/battery-research.md.
 *
 * Battery level is NOT the battery byte of WATCH_CONDITION (0x28), two watches reporting 0x0F
 * there were shown as 2/5 and 3/5 in the official app.
 *
 * @property timestamp When the status was taken, only in the 171-byte layout.
 * @property batteryLevel Null if the byte isn't a known level yet, see [BatteryLevel].
 */
data class WatchStatus(
	val timestamp: LocalDateTime?,
	val batteryLevel: BatteryLevel?,
) {
	/**
	 * @property size Reply size in bytes.
	 * @property batteryIndex Index of the battery level byte, the bars shown in the official app.
	 * @property hasTimestamp Whether the reply starts with a BCD yy mm dd hh mm timestamp.
	 */
	private enum class Layout(val size: Int, val batteryIndex: Int, val hasTimestamp: Boolean) {
		/** GMW-B5000, GW-B5600, e.g. 26 10 16 22 41 01 01 12 … (2026-10-16 22:41, battery 12). */
		CLASSIC(size = 0xAB, batteryIndex = 7, hasTimestamp = true),

		/** GA-B2100, starts 00 05 then an empty log, the tail matches the MIP layout's. */
		CASIOAK(size = 0xAC, batteryIndex = 110, hasTimestamp = false),

		/** GW-BX5600, hourly slots, then battery and temperature. */
		MIP(size = 0xD8, batteryIndex = 158, hasTimestamp = false),
	}

	companion object {
		/**
		 * Size sent in the request. The official app sends each watch its own size, but the watch
		 * ignores it, the GW-BX5600 replied with 216 bytes to this one.
		 */
		private const val REQUEST_SIZE = 0xAB
		private const val DATA_TYPE: Byte = 0x05

		/**
		 * Requests the watch status, the last 3 bytes look like the expected size (little endian).
		 * E.g. 00 05 AB 00 00
		 */
		val requestPacket: ByteArray
			get() = byteArrayOf(WatchStatusSession.START.byte, DATA_TYPE, REQUEST_SIZE.toByte(), 0x00, 0x00)

		/**
		 * Sent by the official app after receiving the watch status.
		 * E.g. 04 05 AB 00 00
		 */
		val endPacket: ByteArray
			get() = byteArrayOf(WatchStatusSession.END.byte, DATA_TYPE, REQUEST_SIZE.toByte(), 0x00, 0x00)

		/**
		 * @throws IllegalArgumentException if the size matches no known [Layout], e.g. a truncated
		 *         reply.
		 */
		fun fromPacket(packet: ByteArray): WatchStatus {
			val layout = requireNotNull(Layout.entries.firstOrNull { it.size == packet.size }) {
				"Unknown watch status size ${packet.size}, expected one of " +
					"${Layout.entries.map { it.size }}, is the MTU too small?"
			}
			return WatchStatus(
				timestamp = if (layout.hasTimestamp) {
					LocalDateTime(
						date = LocalDate.fromBcdByteArray(packet.copyOfRange(0, 3)),
						time = LocalTime(Int.fromBcdByte(packet[3]), Int.fromBcdByte(packet[4])),
					)
				} else null,
				batteryLevel = BatteryLevel.fromByte(packet[layout.batteryIndex]),
			)
		}
	}
}
