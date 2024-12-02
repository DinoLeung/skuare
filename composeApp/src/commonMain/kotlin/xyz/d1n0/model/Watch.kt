package xyz.d1n0.model

import com.benasher44.uuid.uuidFrom
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging
import kotlinx.coroutines.*
import xyz.d1n0.constant.Command

class Watch(private val peripheral: Peripheral) {

	companion object {
		val CASIO_SERVICE_UUID = uuidFrom("00001804-0000-1000-8000-00805f9b34fb")
		private val ALL_FEATURES_CHARACTERISTIC_UUID = "26eb000d-b012-49a8-b1f8-394fb2032b0f"
		private val REQUEST_CHARACTERISTIC_UUID = "26eb002c-b012-49a8-b1f8-394fb2032b0f"
		private val IO_CHARACTERISTIC_UUID = "26eb002d-b012-49a8-b1f8-394fb2032b0f"

		private val requestCharacteristic =
			characteristicOf(service = ALL_FEATURES_CHARACTERISTIC_UUID, characteristic = REQUEST_CHARACTERISTIC_UUID)
		private val ioCharacteristic =
			characteristicOf(service = ALL_FEATURES_CHARACTERISTIC_UUID, characteristic = IO_CHARACTERISTIC_UUID)

		val scanner by lazy {
			Scanner {
				logging {
					level = Logging.Level.Events
				}
				filters {
					match {
						services = listOf(CASIO_SERVICE_UUID)
					}
				}
			}
		}
	}

	init {
		CoroutineScope(Dispatchers.IO).launch {
			startObservingIoCharacteristic()
		}
	}

	private val config = Config()

	private val ioCharacteristicObservation = peripheral.observe(ioCharacteristic)

	suspend fun connect() = peripheral.connect()

	suspend fun disconnect() = peripheral.disconnect()

	/**
	 * Sends a request to the peripheral with a specified command,.
	 * Results will be returned through the IO characteristic.
	 *
	 * @param command The command to be sent to the peripheral.
	 * @return A [Unit] that completes when the command is written successfully.
	 */
	suspend fun request(command: Command) =
		peripheral.write(requestCharacteristic, ByteArray(1) { command.value.toByte() })

	/**
	 * Sends a request to the peripheral with a specified command and position.
	 * Results will be returned through the IO characteristic.
	 *
	 * @param command The command to be sent to the peripheral.
	 * @param position The position value accompanying the command.
	 * @return A [Unit] that completes when the command and position are written successfully.
	 */
	suspend fun request(command: Command, position: Int) =
		peripheral.write(requestCharacteristic, byteArrayOf(command.value.toByte(), position.toByte()))

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
	suspend fun write(data: ByteArray) = peripheral.write(ioCharacteristic, data)

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
	suspend fun startObservingIoCharacteristic() = withContext(Dispatchers.IO) {
		ioCharacteristicObservation.collect {
			when (Command.fromValue(it.first().toInt())) {
				Command.CLOCK -> {
					config.parseClocksPacket(it)
				}
				// TODO: implement other commands
				else -> {
					print("Unsupported packet: ${it.toHexString(HexFormat.UpperCase)}")
				}
			}
		}
	}


}
