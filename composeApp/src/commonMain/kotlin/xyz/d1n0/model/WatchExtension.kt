package xyz.d1n0.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import xyz.d1n0.constant.OpCode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Synchronizes the watch's time, optionally including time zone data.
 *
 * This function:
 * 1. Ensures clock settings are initialized, requesting them if necessary (with a 10-second timeout).
 * 2. Writes current clock settings to the watch.
 * 3. Optionally writes time zone configuration, names, and coordinates.
 * 4. Sends the current date and time (optionally delayed) to the watch.
 *
 * @param delay Optional delay to apply to the current time before syncing (default is 0).
 * @param writeTimezoneData If true, time zone-related data will also be written (default is false).
 */
suspend fun Watch.adjustTime(delay: Duration = 0.seconds, writeTimezoneData: Boolean = false) = runCatching {
    if (clocks.isInitialized == false) {
        requestClocks()
        withTimeoutOrNull(10.seconds) {
            while (clocks.isInitialized == false) {
                delay(100.milliseconds)
            }
        } ?: throw IllegalStateException("Timeout waiting for clocks initialization")
    }
    writeClocks()
    if (writeTimezoneData) {
        writeTimeZoneConfigs()
        writeTimeZoneNames()
        writeTimeZoneCoordinatesAndRadioId()
    }
    writeTime(delay = delay)
}.onSuccess {
    println("Time sync completed")
}.onFailure { error ->
    println("Error syncing time: ${error.message}")
}

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
