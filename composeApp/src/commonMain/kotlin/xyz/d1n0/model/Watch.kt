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
			listen()
		}
	}

	private val config = Config()

	suspend fun connect() = runBlocking {
		peripheral.connect()
	}

	suspend fun disconnect() {
		peripheral.disconnect()
	}

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
	 * Listens to the peripheral for incoming data packets and processes them.
	 * This function observes the IO characteristic, collecting packets of data as they arrive.
	 * Based on the command type parsed from the packet, it either processes clock data or handles
	 * unsupported packets by printing a message.
	 *
	 * Usage of this function requires an established connection to the peripheral.
	 * It is designed to operate within a coroutine scope due to its suspend nature.
	 */
	@OptIn(ExperimentalStdlibApi::class)
	suspend fun listen() {
		peripheral.observe(ioCharacteristic).collect {
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
