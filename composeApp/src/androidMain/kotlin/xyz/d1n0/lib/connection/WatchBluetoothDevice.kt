package xyz.d1n0.lib.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

/**
 * Gets the [BluetoothDevice] for a watch address, e.g. from a stored
 * [xyz.d1n0.data.database.PairedWatch].
 *
 * Casio watches use a random static address, it is passed along so Android doesn't have to guess
 * the address type.
 *
 * @param address BLE address of the watch, e.g. ED:A1:AF:D5:A2:40.
 */
fun Context.watchBluetoothDevice(address: String): BluetoothDevice =
	getSystemService(BluetoothManager::class.java).adapter
		.getRemoteLeDevice(address.uppercase(), BluetoothDevice.ADDRESS_TYPE_RANDOM)
