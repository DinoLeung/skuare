package xyz.d1n0.lib.model

class WatchInfo {
    lateinit var name: WatchName
    lateinit var watchSettings: WatchSettings
	lateinit var connectionSettings: ConnectionSettings

    fun parseNamePacket(packet: ByteArray) {
        name = WatchName.fromPacket(packet)
    }

	fun parseWatchSettingsPacket(packet: ByteArray) {
		watchSettings = WatchSettings.fromPacket(packet)
	}

    fun parseConnectionSettingsPacket(packet: ByteArray) {
        connectionSettings = ConnectionSettings.fromPacket(packet)
    }

}


