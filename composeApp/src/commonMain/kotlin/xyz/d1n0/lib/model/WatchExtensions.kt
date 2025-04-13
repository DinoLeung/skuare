package xyz.d1n0.lib.model

import kotlinx.coroutines.delay
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
suspend fun Watch.writeConnectionSettings() = write(info.connectionSettings.packet)

suspend fun Watch.requestWatchSettings() = request(OpCode.WATCH_SETTINGS)
suspend fun Watch.writeWatchSettings() = write(info.watchSettings.packet)

suspend fun Watch.requestName() = request(OpCode.WATCH_NAME)
suspend fun Watch.WriteName() = write(info.name.packet)

suspend fun Watch.requestTimer() = request(OpCode.TIMER)
suspend fun Watch.writeTimer() = write(timer.timerPacket)

suspend fun Watch.requestClocks() =
    repeat(3) {
        log.d { "requestClocks" }
        request(OpCode.CLOCK)
    }
suspend fun Watch.writeClocks() =
    clocks.clocksPackets.forEach {
        write(it)
    }

suspend fun Watch.writeTime(delay: Duration = 0.seconds) =
    write(clocks.homeClock.getCurrentDateTimePacket(delay = delay))

suspend fun Watch.requestAlarms() {
    request(OpCode.ALARM_A)
    request(OpCode.ALARM_B)
}

suspend fun Watch.writeAlarms() {
    write(alarms.alarmAPacket)
    write(alarms.alarmBPacket)
}

suspend fun Watch.requestTimeZoneConfigs() {
    for(i in 0..5) {
        request(OpCode.TIMEZONE_CONFIG, i)
    }
}
suspend fun Watch.writeTimeZoneConfigs() =
    clocks.timeZoneConfigPackets.forEach {
        write(it)
    }

suspend fun Watch.requestTimeZoneNames() {
    for(i in 0..5) {
        request(OpCode.TIMEZONE_NAME, i)
    }
}
suspend fun Watch.writeTimeZoneNames() =
    clocks.timeZoneNamePackets.forEach {
        write(it)
    }

suspend fun Watch.requestTimeZoneCoordinatesAndRadioId() {
    for(i in 0..5) {
        request(OpCode.TIMEZONE_LOCATION_RADIO_ID, i)
    }
}
suspend fun Watch.writeTimeZoneCoordinatesAndRadioId() =
    clocks.coordinatesRadioIdPackets.forEach {
        write(it)
    }

suspend fun Watch.requestReminderTitles() {
    for(i in 1..5) {
        request(OpCode.REMINDER_TITLE, i)
    }
}
suspend fun Watch.writeReminderTitles() {
    reminders.reminderTitlePackets.forEach {
        write(it)
    }
}

suspend fun Watch.requestReminderConfigs() {
    for(i in 1..5) {
        request(OpCode.REMINDER_CONFIG, i)
    }
}
suspend fun Watch.writeReminderConfigs() {
    reminders.reminderConfigPackets.forEach {
        write(it)
    }
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
suspend fun Watch.adjustTime(delay: Duration = 0.seconds, writeTimezoneMetadata: Boolean = false) = runCatching {
    if (clocks.isInitialized.value == false) {
        requestClocks()
        withTimeoutOrNull(10.seconds) {
            while (clocks.isInitialized.value == false) {
                delay(100.milliseconds)
            }
        } ?: throw IllegalStateException("Timeout waiting for clocks initialization")
    }
    writeClocks()
    writeTimeZoneConfigs()
    writeTimeZoneNames()
    if (writeTimezoneMetadata)
        writeTimeZoneCoordinatesAndRadioId()
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
 *
 * @param packet A [ByteArray] containing the raw BLE packet data received from the peripheral.
 */
@OptIn(ExperimentalStdlibApi::class)
suspend fun Watch.handlePacket(packet: ByteArray) = when (OpCode.fromByte(packet.first())) {
    OpCode.CONNECT_REASON -> {
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
            info.parseConnectionSettingsPacket(packet)
        }.onFailure { log.e { "Failed to parse auto sync settings packet: ${it.message}" } }
    }
    OpCode.WATCH_SETTINGS -> {
        runCatching {
            info.parseWatchSettingsPacket(packet)
        }.onFailure { log.e { "Failed to parse settings packet: ${it.message}" } }
    }
    OpCode.WATCH_NAME -> {
        runCatching {
            info.parseNamePacket(packet)
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
        log.d{ "Battery level: $batteryLevel"}
        log.d { packet.toHexString(HexFormat.UpperCase) }
    }
    OpCode.CLOCK -> {
        runCatching {
            log.d { packet.toHexString(HexFormat.UpperCase) }
            clocks.parseClocksPacket(packet)
        }.onFailure { log.e { "Failed to parse clocks packet: ${it.message}" } }
    }
    OpCode.TIMEZONE_NAME -> { /* Do Nothing */}
    OpCode.TIMEZONE_CONFIG -> { /* Do Nothing */ }
    OpCode.TIMEZONE_LOCATION_RADIO_ID -> { /* Do Nothing */ }
    OpCode.ALARM_A -> {
        runCatching {
            alarms.parseAlarmAPacket(packet)
        }.onFailure { log.e { "Failed to parse alarm A packet: ${it.message}" } }
    }
    OpCode.ALARM_B -> {
        runCatching {
            alarms.parseAlarmBPacket(packet)
        }.onFailure { log.e { "Failed to parse alarm B packet: ${it.message}" } }
    }
    OpCode.TIMER -> {
        runCatching {
            timer.parseTimerPacket(packet)
        }.onFailure { log.e { "Failed to parse timer packet: ${it.message}" } }
    }
    OpCode.REMINDER_TITLE -> {
        runCatching {
            reminders.parseReminderTitlePacket(packet)
        }.onFailure { log.e { "Failed to parse reminder title packet: ${it.message}" } }
    }
    OpCode.REMINDER_CONFIG -> {
        runCatching {
            reminders.parseReminderConfigPacket(packet)
        }.onFailure { log.e { "Failed to parse reminder config packet: ${it.message}" } }
    }
    OpCode.ERROR -> log.e { "Error: ${packet.toHexString(HexFormat.UpperCase)}" }
    else -> log.e { "Unsupported packet: ${packet.toHexString(HexFormat.UpperCase)}" }
}
