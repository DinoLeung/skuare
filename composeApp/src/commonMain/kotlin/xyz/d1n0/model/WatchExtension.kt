package xyz.d1n0.model

import xyz.d1n0.constant.Command
import kotlin.time.Duration.Companion.seconds

suspend fun Watch.requestConnectReason() = request(Command.CONNECT_REASON)

suspend fun Watch.requestAppInfo() = request(Command.APP_INFO)

suspend fun Watch.requestWatchCondition() = request(Command.WATCH_CONDITION)

suspend fun Watch.requestConnectionSettings() = request(Command.CONNECTION_SETTINGS)
// TODO: suspend fun Watch.writeConnectionSettings()

suspend fun Watch.requestWatchSettings() = request(Command.WATCH_SETTINGS)
// TODO: suspend fun Watch.writeWatchSettings()

suspend fun Watch.requestName() = request(Command.WATCH_NAME)
// TODO: suspend fun Watch.writeName()

suspend fun Watch.requestTimer() = request(Command.TIMER)
suspend fun Watch.writeTimer() = write(timer.timerPacket)

suspend fun Watch.requestClocks() =
    repeat(3) {
        request(Command.CLOCK)
    }

suspend fun Watch.writeClocks() =
    clocks.clocksPackets.forEach {
        write(it)
    }

suspend fun Watch.requestAlarms() {
    request(Command.ALARM_A)
    request(Command.ALARM_B)
}

suspend fun Watch.writeAlarms() {
    write(alarms.alarmAPacket)
    write(alarms.alarmBPacket)
}

// TODO: suspend fun Watch.requestTimeZoneConfigs()
suspend fun Watch.writeTimeZoneConfigs() =
    clocks.timeZoneConfigPackets.forEach {
        write(it)
    }

// TODO: suspend fun Watch.requestTimeZoneNames()
suspend fun Watch.writeTimeZoneNames() =
    clocks.timeZoneNamePackets.forEach {
        write(it)
    }

suspend fun Watch.requestTimeZoneCoordinatesAndRadioId() {
    for(i in 0..5) {
        request(Command.TIMEZONE_LOCATION_RADIO_ID, i)
    }
}

suspend fun Watch.writeTimeZoneCoordinatesAndRadioId() =
    clocks.coordinatesRadioIdPackets.forEach {
        write(it)
    }

suspend fun Watch.writeTime() =
    write(clocks.homeClock.getCurrentDateTimePacket(delay = 0.seconds))
