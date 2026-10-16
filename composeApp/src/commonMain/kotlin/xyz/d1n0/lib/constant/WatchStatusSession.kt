package xyz.d1n0.lib.constant

/**
 * First byte of the packets that start and end a watch status request, written to the data request
 * characteristic, as seen from the official app.
 * Real names unknown.
 */
enum class WatchStatusSession(val byte: Byte) {
	START(0x00),
	END(0x04);
}
