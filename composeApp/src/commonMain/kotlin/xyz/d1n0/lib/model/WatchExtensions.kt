package xyz.d1n0.lib.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import xyz.d1n0.lib.constant.OpCode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun Watch.requestConnectReason() = request(OpCode.CONNECT_REASON)

suspend fun Watch.requestAppInfo() = request(OpCode.APP_INFO)
suspend fun Watch.writeAppInfo(appInfo: AppInfo) = write(appInfo.packet).also { requestAppInfo() }

suspend fun Watch.requestWatchCondition() = request(OpCode.WATCH_CONDITION)

/**
 * Requests the watch status, which holds the battery level shown in the official app, and stores it
 * in [Watch.info].
 *
 * @return A [Result] that fails if the transfer times out or the watch status could not be parsed.
 */
suspend fun Watch.requestWatchStatus() =
	runCatching {
		val packet = requestData(WatchStatus.requestPacket, WatchStatus.endPacket)
		_info.update { it.parseWatchStatusPacket(packet) }
	}.onSuccess {
		log.d { "Watch status: ${info.value.watchStatus}, battery level: ${info.value.watchStatus?.batteryLevel?.bars}/5" }
	}.onFailure { error ->
		log.e { "Error requesting watch status: ${error.message}" }
	}

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

suspend fun Watch.writeClocks(clocksSettings: ClocksSettings) =
	clocksSettings.clocksPackets.forEach { write(it) }

suspend fun Watch.writeTime(delay: Duration = 0.seconds) =
	clocks.value.homeClock?.let { write(it.getCurrentDateTimePacket(delay = delay)) }

suspend fun Watch.requestAlarms() {
	request(OpCode.ALARM_A)
	request(OpCode.ALARM_B)
}

suspend fun Watch.writeAlarms(alarmSettings: AlarmsSettings) {
	write(alarmSettings.alarmAPacket)
	write(alarmSettings.alarmBPacket)
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
 * Writes clocks and their time zone data to the watch, then reads the clocks back.
 *
 * Clock and time zone writes must be wrapped in a clocks update session (`21 00 01` … `21 01 01`),
 * as the official app does, otherwise requesting clocks afterwards errors. The end packet also
 * persists the changes on the watch, so no separate time write is needed.
 *
 * This function:
 * 1. Starts the clocks update session.
 * 2. Writes clocks, time zone configs, time zone names, then coordinates and radio IDs.
 * 3. Ends the clocks update session.
 * 4. Requests clocks so [Watch.clocks] reflects what the watch saved.
 *
 * Packets are built by each write as it runs, so if one cannot be built (e.g. an uninitialized
 * clock) the update session has already started and is left open.
 *
 * @param clocksSettings The clocks to write, all six clocks must be initialized.
 * @return A [Result] that fails if a packet could not be built or written, or if requesting
 *         clocks afterwards fails.
 */
suspend fun Watch.writeClocksTimezones(clocksSettings: ClocksSettings) =
	runCatching {
		startClockUpdateSession()
		writeClocks(clocksSettings)
		writeTimeZoneConfigs(clocksSettings)
		writeTimeZoneNames(clocksSettings)
		writeTimeZoneCoordinatesAndRadioId(clocksSettings)
		endClockUpdateSession()

		requestClocks()
//		requestTimeZoneConfigs()
//		requestTimeZoneNames()
//		requestTimeZoneCoordinatesAndRadioId()
	}.onSuccess {
		log.d { "Clocks and time zones written" }
	}.onFailure { error ->
		log.e { "Error writing clocks and time zones: ${error.message}" }
	}

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

suspend fun Watch.startClockUpdateSession() = write(ClocksSettings.clocksUpdateSessionStartPacket)

suspend fun Watch.endClockUpdateSession() = write(ClocksSettings.clocksUpdateSessionEndPacket)

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
		runCatching {
			_info.update { it.parseConnectReasonPacket(packet) }
		}.onFailure { log.e { "Failed to parse connect reason packet: ${it.message}" } }
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
		runCatching {
			_info.update { it.parseAppInfoPacket(packet) }
		}.onFailure { log.e { "Failed to parse app info packet: ${it.message}" } }
	}

	OpCode.WATCH_CONDITION -> {
		// 28 13 1A 01 (4/5 battery in official app)
		// 28 0F 1D 00 (2/5 battery in official app)
		// 28 0F 19 00 (3/5 battery in official app)
		// battery byte does not match the official app, which uses the watch status instead, see WatchStatus
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
