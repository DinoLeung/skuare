package xyz.d1n0.model

class WatchInfo {
    lateinit var name: WatchName
    lateinit var settings: WatchSettings
	lateinit var autoSyncSettings: AutoSyncSettings

    // TODO: connection timeout

    fun parseNamePacket(namePacket: ByteArray) {
        name = WatchName.fromPacket(namePacket)
    }

	fun parseSettingsPacket(settingsPacket: ByteArray) {
		settings = WatchSettings.fromPacket(settingsPacket)
	}

    fun parseAutoSyncPacket(autoSyncPacket: ByteArray) {
        autoSyncSettings = AutoSyncSettings.fromPacket(autoSyncPacket)
    }

}


