package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.OpCode

/**
 * Framing of the batched characteristics, which carry several world time packets at once. Each
 * record is the same packet as on the IO characteristic (`1D`, `1E`, `1F`, `24`).
 *
 * - Request, written to the batch request characteristic:
 *   `count, (opcode, index) × count`, e.g. 05 1D00 1D00 2400 2401 2402
 * - Reply (notification) and write, on the batch transfer characteristic:
 *   `count, (length16 LE, record) × count`, e.g. 02 0F00 1D00… 0F00 1D02…
 *
 * For `1D` and `1E` requests the watch ignores the index and walks the records itself, so
 * requesting `1D00` twice gets `1D00` and `1D02`.
 */
object BatchPacket {
	/**
	 * @param requests Opcode and index of each record to request.
	 * @throws IllegalArgumentException if there are no requests or more than 255.
	 */
	fun request(requests: List<Pair<OpCode, Int>>): ByteArray {
		require(requests.size in 1..255) { "A batch holds 1 to 255 requests, got ${requests.size}" }
		return requests.fold(byteArrayOf(requests.size.toByte())) { batch, (opCode, index) ->
			batch + byteArrayOf(opCode.byte, index.toByte())
		}
	}

	/**
	 * @param records The packets to write, e.g. [ClocksSettings.clocksPackets].
	 * @throws IllegalArgumentException if there are no records or more than 255.
	 */
	fun encode(records: List<ByteArray>): ByteArray {
		require(records.size in 1..255) { "A batch holds 1 to 255 records, got ${records.size}" }
		return records.fold(byteArrayOf(records.size.toByte())) { batch, record ->
			batch + byteArrayOf(record.size.toByte(), (record.size shr 8).toByte()) + record
		}
	}

	/**
	 * @param batch A batch the watch sent on the batch transfer characteristic.
	 * @return The records in the order the watch sent them.
	 * @throws IllegalArgumentException if the batch is cut short, e.g. by a small MTU, or has
	 *         bytes after the last record.
	 */
	fun decode(batch: ByteArray): List<ByteArray> {
		require(batch.isNotEmpty()) { "Batch is empty" }
		val count = batch[0].toInt() and 0xFF
		var offset = 1
		val records = List(count) { i ->
			require(offset + 2 <= batch.size) {
				"Batch ends before record ${i + 1} of $count, is the MTU too small?"
			}
			val length = (batch[offset].toInt() and 0xFF) or ((batch[offset + 1].toInt() and 0xFF) shl 8)
			offset += 2
			require(offset + length <= batch.size) {
				"Batch ends inside record ${i + 1} of $count, is the MTU too small?"
			}
			batch.copyOfRange(offset, offset + length).also { offset += length }
		}
		require(offset == batch.size) { "Batch has ${batch.size - offset} bytes after its $count records" }
		return records
	}
}
