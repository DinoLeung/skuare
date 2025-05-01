package xyz.d1n0.lib.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import xyz.d1n0.lib.constant.ConnectReason
import xyz.d1n0.lib.constant.OpCode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun Watch.requestConnectReason() = request(OpCode.CONNECT_REASON)

suspend fun Watch.requestAppInfo() = request(OpCode.APP_INFO)

suspend fun Watch.requestWatchCondition() = request(OpCode.WATCH_CONDITION)

suspend fun Watch.requestConnectionSettings() = request(OpCode.CONNECTION_SETTINGS)
suspend fun Watch.writeConnectionSettings(connectionSettings: ConnectionSettings) =
	write(connectionSettings.packet).also { requestConnectionSettings() }

suspend fun Watch.requestWatchSettings() = request(OpCode.WATCH_SETTINGS)
suspend fun Watch.writeWatchSettings(watchSettings: WatchSettings) =
	write(watchSettings.packet).also { requestWatchSettings() }

suspend fun Watch.requestName() = request(OpCode.WATCH_NAME)
suspend fun Watch.writeName(watchName: WatchName) = write(watchName.packet).also { requestName() }

suspend fun Watch.requestTimer() = request(OpCode.TIMER)
suspend fun Watch.writeTimer(timer: Timer) = write(timer.packet).also { requestTimer() }

suspend fun Watch.requestClocks() = repeat(3) {
	log.d { "requestClocks" }
	request(OpCode.CLOCK)
}

// TODO:
suspend fun Watch.writeClocks(clocksSettings: ClocksSettings) =
	clocksSettings.clocksPackets.forEach { write(it) }.also { requestClocks() }

suspend fun Watch.writeTime(delay: Duration = 0.seconds) =
	clocks.value.homeClock?.let { write(it.getCurrentDateTimePacket(delay = delay)) }

suspend fun Watch.requestAlarms() {
	request(OpCode.ALARM_A)
	request(OpCode.ALARM_B)
}

suspend fun Watch.writeAlarms() {
	write(alarms.value.alarmAPacket)
	write(alarms.value.alarmBPacket)
	requestAlarms()
}

suspend fun Watch.requestTimeZoneConfigs() {
	for (i in 0..5) {
		request(OpCode.TIMEZONE_CONFIG, i)
	}
}

suspend fun Watch.writeTimeZoneConfigs(clocksSettings: ClocksSettings) =
	clocksSettings.timeZoneConfigPackets.forEach { write(it) }
//        .also { requestTimeZoneConfigs() }

suspend fun Watch.requestTimeZoneNames() {
	for (i in 0..5) {
		request(OpCode.TIMEZONE_NAME, i)
	}
}

suspend fun Watch.writeTimeZoneNames(clocksSettings: ClocksSettings) =
	clocksSettings.timeZoneNamePackets.forEach { write(it) }
//        .also { requestTimeZoneNames() }

suspend fun Watch.requestTimeZoneCoordinatesAndRadioId() {
	for (i in 0..5) {
		request(OpCode.TIMEZONE_LOCATION_RADIO_ID, i)
	}
}

suspend fun Watch.writeTimeZoneCoordinatesAndRadioId(clocksSettings: ClocksSettings) =
	clocksSettings.coordinatesRadioIdPackets.forEach { write(it) }
//        .also { requestTimeZoneNames() }

suspend fun Watch.requestReminderTitles() {
	for (i in 1..5) {
		request(OpCode.REMINDER_TITLE, i)
	}
}

suspend fun Watch.writeReminderTitles(remindersSettings: RemindersSettings) =
	remindersSettings.reminderTitlePackets.forEach { write(it) }.also { requestReminderTitles() }


suspend fun Watch.requestReminderConfigs() {
	for (i in 1..5) {
		request(OpCode.REMINDER_CONFIG, i)
	}
}

suspend fun Watch.writeReminderConfigs(remindersSettings: RemindersSettings) =
	remindersSettings.reminderConfigPackets.forEach { write(it) }.also { requestReminderConfigs() }

/**
 * Synchronizes the watch's time, optionally including time zone data.
 *
 * This function:
 * 1. Ensures clock settings are initialized, requesting them if necessary (with a 10-second timeout).
 * 2. Writes current clock settings to the watch.
 * 3. Optionally writes time zone radio configuration and coordinates.
 * 4. Sends the current date and time (optionally delayed) to the watch.
 *
 * @param delay Optional delay to apply to the current time before syncing (default is 0).
 * @param writeTimezoneMetadata If true, timezone metadata will also be written (default is false).
 */
suspend fun Watch.adjustTime(delay: Duration = 0.seconds, writeTimezoneMetadata: Boolean = false) =
	runCatching {
		if (clocks.value.isInitialized == false) {
			requestClocks()
			withTimeoutOrNull(10.seconds) {
				while (clocks.value.isInitialized == false) {
					delay(100.milliseconds)
				}
			} ?: throw IllegalStateException("Timeout waiting for clocks initialization")
		}
		writeClocks(clocks.value)
		writeTimeZoneConfigs(clocks.value)
		writeTimeZoneNames(clocks.value)
		if (writeTimezoneMetadata) writeTimeZoneCoordinatesAndRadioId(clocks.value)
		writeTime(delay = delay)
	}.onSuccess {
		log.d { "Time sync completed" }
	}.onFailure { error ->
		log.e { "Error syncing time: ${error.message}" }
	}

/**
 * Processes an incoming BLE packet by dispatching it based on its opcode.
 *
 * This suspend function extracts the opcode from the first byte of the [packet] and routes the packet
 * to the corresponding handler. Depending on the opcode, it will attempt to parse the packet data using
 * various parsing methods.
 *
 * If an error occurs during parsing, the function logs an appropriate error message.
 *
 * TODO: setup proper logging
 * TODO: now all top level properties are MutableStateFlow, should make all underlying properties val
 *
 * @param packet A [ByteArray] containing the raw BLE packet data received from the peripheral.
 */
@OptIn(ExperimentalStdlibApi::class)
suspend fun Watch.handlePacket(packet: ByteArray) = when (OpCode.fromByte(packet.first())) {
	OpCode.CONNECT_REASON -> {
		_info.update { it.parseConnectReasonPacket(packet) }
		val reason = ConnectReason.fromByte(packet[8])
		when (reason) {
			ConnectReason.SETUP, ConnectReason.DEFAULT -> {
				// TODO: Check APP_INFO, up date if required
			}

			ConnectReason.AUTO_SYNC, ConnectReason.MANUAL_SYNC -> {
				// TODO: sync time procedure, then disconnect
				// TODO: figure out how long the watch will stay connected for
			}

			ConnectReason.FIND -> {
				// TODO: figure out if sync time is required
				// TODO: figure out how long the watch will stay connected for
			}
		}

	}

	OpCode.CONNECTION_SETTINGS -> {
		runCatching {
			_info.update { it.parseConnectionSettingsPacket(packet) }
		}.onFailure { log.e { "Failed to parse auto sync settings packet: ${it.message}" } }
	}

	OpCode.WATCH_SETTINGS -> {
		runCatching {
			_info.update { it.parseWatchSettingsPacket(packet) }
		}.onFailure { log.e { "Failed to parse settings packet: ${it.message}" } }
	}

	OpCode.WATCH_NAME -> {
		runCatching {
			_info.update { it.parseNamePacket(packet) }
		}.onFailure { log.e { "Failed to parse name packet: ${it.message}" } }
	}

	OpCode.APP_INFO -> {
		// new/reset watch		22FFFFFFFFFFFFFFFFFFFF00
		// b5600 old app ver.	222DA85E248C468C74834202
		// b5600 newer app ver.	228C8973A1B416502E67DD02
		// Looks like the app will compare this number to determine if the watch has been paired or not
		// probably can set to anything except the last byte.
		log.d { packet.toHexString(HexFormat.UpperCase) }
	}

	OpCode.WATCH_CONDITION -> {
		// 28 13 1F 00
		// 28 13 1A 00 (4/5 battery)
		val BATTERY_MAX = 0x13
		val BATTERY_MIN = 0x09 // ???
		val batteryLevel = (packet[1].toFloat() - BATTERY_MIN) / (BATTERY_MAX - BATTERY_MIN)
		log.d { "Battery level: $batteryLevel" }
		log.d { packet.toHexString(HexFormat.UpperCase) }
	}

	OpCode.CLOCK -> {
		runCatching {
			log.d { packet.toHexString(HexFormat.UpperCase) }
			_clocks.update { it.parseClocksPacket(packet) }
		}.onFailure { log.e { "Failed to parse clocks packet: ${it.message}" } }
	}

	OpCode.TIMEZONE_NAME -> { /* Do Nothing */
	}

	OpCode.TIMEZONE_CONFIG -> { /* Do Nothing */
	}

	OpCode.TIMEZONE_LOCATION_RADIO_ID -> { /* Do Nothing */
	}

	OpCode.ALARM_A -> {
		runCatching {
			_alarms.update { it.parseAlarmAPacket(packet) }
		}.onFailure { log.e { "Failed to parse alarm A packet: ${it.message}" } }
	}

	OpCode.ALARM_B -> {
		runCatching {
			_alarms.update { it.parseAlarmBPacket(packet) }
		}.onFailure { log.e { "Failed to parse alarm B packet: ${it.message}" } }
	}

	OpCode.TIMER -> {
		runCatching {
			_timer.update { it.parseTimerPacket(packet) }
		}.onFailure { log.e { "Failed to parse timer packet: ${it.message}" } }
	}

	OpCode.REMINDER_TITLE -> {
		runCatching {
			_reminders.update { it.parseReminderTitlePacket(packet) }
		}.onFailure { log.e { "Failed to parse reminder title packet: ${it.message}" } }
	}

	OpCode.REMINDER_CONFIG -> {
		runCatching {
			// it returns 3101000000000000000000 on new watches
			_reminders.update { it.parseReminderConfigPacket(packet) }
		}.onFailure { log.e { "Failed to parse reminder config packet: ${it.message}" } }
	}

	OpCode.ERROR -> log.e { "Error: ${packet.toHexString(HexFormat.UpperCase)}" }
	else -> log.e { "Unsupported packet: ${packet.toHexString(HexFormat.UpperCase)}" }
}
