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
 * Known layout, e.g. 26 10 16 22 41 01 01 12 86 00 00 00 12 12 12 ... (171 bytes):
 * [0..4] timestamp, BCD yy mm dd hh mm (2026-10-16 22:41)
 * [5..6] unknown, 01 01
 * [7] battery level, matches the bars shown in the official app, see [batteryLevel]
 * [12..23] repeats the battery level, probably a battery history
 * rest unknown, ends with log entries of BCD yy mm dd hh mm followed by a flag byte
 *
 * Battery level is NOT the battery byte of WATCH_CONDITION (0x28), two watches reporting 0x0F
 * there were shown as 2/5 and 3/5 in the official app.
 */
data class WatchStatus(
	val timestamp: LocalDateTime,
	val batteryLevel: BatteryLevel,
) {
	companion object {
		const val SIZE = 0xAB
		private const val DATA_TYPE: Byte = 0x05

		/**
		 * Requests the watch status, the last 3 bytes look like the expected size (little endian).
		 * E.g. 00 05 AB 00 00
		 */
		val requestPacket: ByteArray
			get() = byteArrayOf(WatchStatusSession.START.byte, DATA_TYPE, SIZE.toByte(), 0x00, 0x00)

		/**
		 * Sent by the official app after receiving the watch status.
		 * E.g. 04 05 AB 00 00
		 */
		val endPacket: ByteArray
			get() = byteArrayOf(WatchStatusSession.END.byte, DATA_TYPE, SIZE.toByte(), 0x00, 0x00)

		fun fromPacket(packet: ByteArray): WatchStatus {
			require(packet.size == SIZE) {
				"Watch status packet must be exactly $SIZE bytes long, got ${packet.size}, is the MTU too small?"
			}
			return WatchStatus(
				timestamp = LocalDateTime(
					date = LocalDate.fromBcdByteArray(packet.copyOfRange(0, 3)),
					time = LocalTime(Int.fromBcdByte(packet[3]), Int.fromBcdByte(packet[4])),
				),
				batteryLevel = BatteryLevel.fromByte(packet[7]),
			)
		}
	}
}
