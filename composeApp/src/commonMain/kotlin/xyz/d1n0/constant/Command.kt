package xyz.d1n0.constant

enum class Command(val code: Int) {
    watchName(0x23),
    appInformation(0x22),
    bleFeatures(0x10),
    settingForBle(0x11),
    watchCondition(0x28),
    clock(0x1D),
    timeZoneName(0x1F),
    timeZoneConfig(0x1E),

    // somethingRelatedToTimezone(0x24), maybe it's not important
    currentTime(0x09),

    //    settingForAlm(0x15),
//    settingForAlm2(0x16),
//    settingForBasic(0x13),
//    currentTimeManager(0x39),
//    reminderTitle(0x30),
//    reminderTime(0x31),
//    timer(0x18),
    error(0xFF);

    // ECB-30
//    cmdSetTimeMode(0x47),
//    findPhone(0x0A);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: error("Unknown command code: $code")
    }
}
