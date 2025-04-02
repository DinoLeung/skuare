package xyz.d1n0.model

import com.juul.kable.*
import com.juul.kable.logs.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import xyz.d1n0.constant.BleUuid
import xyz.d1n0.constant.OpCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi

class Watch(private val peripheral: Peripheral) {

	@OptIn(ExperimentalUuidApi::class)
	companion object {
		private val requestCharacteristic =
			characteristicOf(service = BleUuid.ALL_FEATURES_CHARACTERISTIC_UUID, characteristic = BleUuid.REQUEST_CHARACTERISTIC_UUID)
		private val ioCharacteristic =
			characteristicOf(service = BleUuid.ALL_FEATURES_CHARACTERISTIC_UUID, characteristic = BleUuid.IO_CHARACTERISTIC_UUID)

		val scanner by lazy {
			Scanner {
				logging {
					level = Logging.Level.Events
				}
				filters {
					match {
						services = listOf(BleUuid.CASIO_SERVICE_UUID)
					}
				}
			}
		}
	}

	val info = WatchInfo()
	val clocks = ClocksSettings()
	val alarms = AlarmsSettings()
	val timer = TimerSettings()
	val reminders = RemindersSettings()

	/**
	 * Provides the [CoroutineScope] associated with the watch connection.
	 * This scope is used to launch coroutines tied to the lifecycle of the peripheral.
	 */
	val scope: CoroutineScope get() = peripheral.scope

	/**
	 * Represents the current connection state of the watch as a [StateFlow].
	 * This can be observed to react to connection state changes, such as connected, disconnected, or connecting.
	 */
	val state: StateFlow<State> get() = peripheral.state

	/**
	 * Observes incoming data from the IO characteristic of the peripheral.
	 * This observation emits incoming BLE packets which are then processed by [observeIoCharacteristic].
	 */
	private val ioCharacteristicObservation = peripheral.observe(ioCharacteristic)

	/**
	 * Starts observing the IO characteristic for incoming data packets.
	 *
	 * This function collects data emitted by `ioCharacteristicObservation` in the IO coroutine context
	 * and processes them based on the command type. If the packet type is unsupported, it will print
	 * the packet data in hexadecimal format.
	 *
	 * This function is a coroutine and should be invoked within a coroutine scope.
	 *
	 * Note: This is an experimental API and usage may require enabling the `ExperimentalStdlibApi` opt-in.
	 */
	@OptIn(ExperimentalStdlibApi::class)
	@Throws(IllegalArgumentException::class, CancellationException::class)
	private suspend fun observeIoCharacteristic() =
		ioCharacteristicObservation.collect { handlePacket(it) }

	/**
	 * Disconnects from the peripheral watch, terminating any ongoing data observations.
	 */
	suspend fun connect() = peripheral.connect()
		.launch { observeIoCharacteristic() }

	/**
	 * Disconnects the BLE connection from the peripheral.
	 */
	suspend fun disconnect() = peripheral.disconnect()

	/**
	 * Sends a request to the peripheral with a specified command and position.
	 * Results will be returned through the IO characteristic.
	 *
	 * @param opCode The command to be sent to the peripheral.
	 * @return A [Unit] that completes when the command and position are written successfully.
	 */
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun request(opCode: OpCode) =
		peripheral.write(requestCharacteristic, byteArrayOf(opCode.byte))

	/**
	 * Sends a request to the peripheral with a specified command and position.
	 * Results will be returned through the IO characteristic.
	 *
	 * @param opCode The command to be sent to the peripheral.
	 * @param position The position value accompanying the command.
	 * @return A [Unit] that completes when the command and position are written successfully.
	 */
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun request(opCode: OpCode, position: Int) =
		peripheral.write(requestCharacteristic, byteArrayOf(opCode.byte, position.toByte()))

	/**
	 * Writes data to the peripheral using the IO characteristic.
	 *
	 * @param data The [ByteArray] containing data to be written to the peripheral.
	 *             This data will be sent over the IO characteristic.
	 *
	 * @return A [Unit] that completes when the data is written successfully.
	 *
	 * Usage of this function requires an established connection to the peripheral.
	 */
	@OptIn(ExperimentalStdlibApi::class)
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun write(data: ByteArray) {
		println("Writing: ${data.toHexString(HexFormat.UpperCase)}")
		peripheral.write(ioCharacteristic, data, WriteType.WithResponse)
	}
}

//class WatchException (message: String, cause: Throwable) : Exception(message, cause)
