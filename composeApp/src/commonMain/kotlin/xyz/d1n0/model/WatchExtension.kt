package xyz.d1n0.model

import xyz.d1n0.constant.Command
import kotlin.time.Duration.Companion.seconds

suspend fun Watch.requestConnectReason() = request(Command.CONNECT_REASON)
suspend fun Watch.requestTimeSyncSettings() = request(Command.CONNECTION_SETTINGS)
suspend fun Watch.requestWatchSettings() = request(Command.WATCH_SETTINGS)
suspend fun Watch.requestAppInfo() = request(Command.APP_INFO)
suspend fun Watch.requestName() = request(Command.WATCH_NAME)
suspend fun Watch.requestWatchCondition() = request(Command.WATCH_CONDITION)
suspend fun Watch.requestTimer() = request(Command.TIMER)

suspend fun Watch.requestClocks() =
    repeat(3) {
        request(Command.CLOCK)
    }

suspend fun Watch.requestAlarms() {
    request(Command.ALARM_A)
    request(Command.ALARM_B)
}

suspend fun Watch.writeClocks() =
    clocks.clocksPackets.forEach {
        write(it)
    }

suspend fun Watch.writeTimeZoneConfigs() =
    clocks.timeZoneConfigPackets.forEach {
        write(it)
    }

suspend fun Watch.writeTimeZoneNames() =
    clocks.timeZoneNamePackets.forEach {
        write(it)
    }

suspend fun Watch.writeTime() =
    write(clocks.homeClock.getCurrentDateTimePacket(delay = 0.seconds))

suspend fun Watch.writeAlarms() {
    write(alarms.alarmAPacket)
    write(alarms.alarmBPacket)
}

suspend fun Watch.writeTimer() = write(timer.timerPacket)