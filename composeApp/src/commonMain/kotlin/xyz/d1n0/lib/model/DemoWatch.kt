package xyz.d1n0.lib.model

import com.juul.kable.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.d1n0.lib.constant.OpCode

/**
 * Stands in for Bluetooth on the demo watch, see [Watch.demo], so the app can be tried without a
 * watch. Nothing is sent anywhere.
 *
 * It keeps the last packet written for each record and replies to a request with it, so a write
 * followed by a request reads back what was written, which is what the screens wait for after a
 * save. Replies have the same layout as the packets written, e.g. 1D 00 01 … both ways. Records
 * start as captured packets, see [initialRecords]. A request for a record it doesn't have gets no
 * reply.
 */
internal class DemoWatch {
	val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	val state = MutableStateFlow<State>(State.Disconnected())

	private val mutex = Mutex()
	private val records: MutableMap<Record, ByteArray> = initialRecords.associateBy(::recordOf).toMutableMap()
	private var nextClockPair = 0

	fun connect() {
		state.value = State.Connected(scope)
	}

	fun disconnect() {
		state.value = State.Disconnected()
	}

	/** Disconnects and cancels [scope], like closing a peripheral. */
	fun close() {
		disconnect()
		scope.cancel()
	}

	/**
	 * Keeps [packet] as its record's latest value.
	 *
	 * @return The reply to the write, null if none. Hand adjust writes get [HandAdjust.ACK] at
	 *         once, the hands have nowhere to move.
	 */
	suspend fun write(packet: ByteArray): ByteArray? {
		if (packet.first() == OpCode.HAND_ADJUST.byte) return HandAdjust.ACK
		mutex.withLock { records[recordOf(packet)] = packet.copyOf() }
		return null
	}

	/**
	 * @param position The position sent with the request, null if none.
	 * @param clockCount The watch's clock count. Clocks are sent a pair per request, walking the
	 *        pairs like the watch does, see [requestClocks].
	 * @return The record's latest packet, null if there is none.
	 */
	suspend fun reply(opCode: OpCode, position: Int?, clockCount: Int): ByteArray? = mutex.withLock {
		val record = if (opCode == OpCode.CLOCK) {
			val pairs = (clockCount + 1) / 2
			val pair = nextClockPair % pairs
			nextClockPair = (pair + 1) % pairs
			Record(opCode.byte, position = pair * 2)
		} else {
			Record(opCode.byte, position)
		}
		records[record]?.copyOf()
	}

	/**
	 * @property position Byte 1 of the packet for opcodes with one record per position, see
	 *           [positionedOpCodes], null for the others.
	 */
	private data class Record(val opCode: Byte, val position: Int?)

	companion object {
		/** Everything on, so every screen and setting can be tried. */
		val model = CustomWatchModel(
			clockCount = 6,
			reminderCount = 5,
			hasFontStyle = true,
			hasShortCityNames = true,
			hasWeekdayLanguage = true,
			hasSnoozeAlarm = true,
			hasTimeZoneLocation = true,
			canAdjustHands = true,
		)

		private val positionedOpCodes = setOf(
			OpCode.CLOCK,
			OpCode.TIMEZONE_CONFIG,
			OpCode.TIMEZONE_NAME,
			OpCode.TIMEZONE_LOCATION_RADIO_ID,
			OpCode.REMINDER_TITLE,
			OpCode.REMINDER_CONFIG,
		).map { it.byte }.toSet()

		private fun recordOf(packet: ByteArray) = Record(
			opCode = packet[0],
			position = if (packet[0] in positionedOpCodes) packet[1].toInt() else null,
		)

		/** The same packets as the model tests, so they are known to parse. */
		private val initialRecords: List<ByteArray> = listOf(
			// GW-B5600 capture with Adelaide (home) pinned back from clock 2, Hong Kong (world) at
			// clock 2, see pinSwapsWholeClocksWithThePinnedClock in ClocksSettingsTest
			"1D000103027F760000FFFFFFFFFFFF",
			"1D020302027A001901FFFFFFFFFFFF",
			"1D040503035F003F00FFFFFFFFFFFF",
			// see AlarmsSettingsTest
			"15C0000C1E",
			"160000011E40000E0F00000F2D4000173B",
			// see TimerTest
			"18170F1E00000002",
			// reminder 1, see RemindersSettingsTest
			"30014D454554494E470000000000000000000000",
			"3101052503252503252200",
			// GW-BX5600, font Standard, see WatchSettingsTest
			"131700000000000000000000",
			// see ConnectionSettingsTest
			"110F0F0F0600500004000100002003",
			// CASIO GW-B5600#, see WatchNameTest
			"23434153494F2047572D42353630302300000000",
		).map { it.hexToByteArray() } +
			// reminders 2 to 5 unset, zero after the position, like 31 01 00 … from a new watch, see handlePacket
			(2..5).flatMap { position ->
				listOf(
					byteArrayOf(OpCode.REMINDER_TITLE.byte, position.toByte()) + ByteArray(18),
					byteArrayOf(OpCode.REMINDER_CONFIG.byte, position.toByte()) + ByteArray(9),
				)
			}
	}
}
