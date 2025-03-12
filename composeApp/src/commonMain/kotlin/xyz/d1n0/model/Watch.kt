package xyz.d1n0.model

import com.juul.kable.*
import com.juul.kable.logs.Logging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.io.IOException
import xyz.d1n0.constant.BleUuid
import xyz.d1n0.constant.Command
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
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

	val clocksConfig = ClocksConfig()

	val scope: CoroutineScope get() = peripheral.scope

	val connectionState: State get() = _connectionState
	private var _connectionState: State = State.Disconnected()


	private val ioCharacteristicObservation = peripheral.observe(ioCharacteristic)

	suspend fun connect() = peripheral.connect().let { coroutineScope ->
		coroutineScope.launch { observeConnectionState() }
		coroutineScope.launch { observeIoCharacteristic() }
	}

	suspend fun disconnect() = peripheral.disconnect().also {
		_connectionState = State.Disconnected()
	}

	/**
	 * Sends a request to the peripheral with a specified command and position.
	 * Results will be returned through the IO characteristic.
	 *
	 * @param command The command to be sent to the peripheral.
	 * @return A [Unit] that completes when the command and position are written successfully.
	 */
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun request(command: Command) =
		peripheral.write(requestCharacteristic, byteArrayOf(command.value.toByte()))

	/**
	 * Sends a request to the peripheral with a specified command and position.
	 * Results will be returned through the IO characteristic.
	 *
	 * @param command The command to be sent to the peripheral.
	 * @param position The position value accompanying the command.
	 * @return A [Unit] that completes when the command and position are written successfully.
	 */
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
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
	@OptIn(ExperimentalStdlibApi::class)
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun write(data: ByteArray) {
		println("Writing: ${data.toHexString(HexFormat.UpperCase)}")
		peripheral.write(ioCharacteristic, data, WriteType.WithResponse)
	}

	private suspend fun observeConnectionState(): Nothing =
		peripheral.state.collect {
			println("startObservingConnectionState ${it.toString()}")
			_connectionState = it
			if (it is State.Disconnected) {
				// TODO: it should somehow clean up the connection, then UI should reflect that
				// if (it.status is State.Disconnected.Status.PeripheralDisconnected) {}
			}
//			when (it) {
//				is State.Connected -> {
//					println("Connected")
//				}
//				is State.Connecting -> {
//					println("Connecting")
//				}
//				is State.Disconnected -> {
//					println("Disconnected")
//				}
//				is State.Disconnecting -> {
//					println("Disconnecting")
//				}
//			}
		}

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
		ioCharacteristicObservation.collect {
			println("ioCharacteristicObservation.collect")
			println(it.toHexString(HexFormat.UpperCase))
			when (Command.fromValue(it.first().toInt())) {
				Command.WATCH_NAME -> {
					val name = it.drop(1)
						.takeWhile { it != 0x00.toByte() }
						.toByteArray()
						.decodeToString()
					println("WATCH_NAME: $name")

				}
				Command.CLOCK -> {
					runCatching {
						clocksConfig.parseClocksPacket(it)
					}.onFailure { println("Failed to parse clocks packet: ${it.message}") }
				}
				// TODO: implement other commands
				else -> {
					println("Unsupported packet: ${it.toHexString(HexFormat.UpperCase)}")
				}
			}
		}

	suspend fun requestName() = request(Command.WATCH_NAME)

	suspend fun requestClocks() =
		repeat(3) {
			request(Command.CLOCK)
		}

	suspend fun writeClocks() =
		clocksConfig.clocksPackets.forEach {
			write(it)
		}

	suspend fun writeTimeZoneConfigs() =
		clocksConfig.timeZoneConfigPackets.forEach {
			write(it)
		}

	suspend fun writeTimeZoneNames() =
		clocksConfig.timeZoneNamePackets.forEach {
			write(it)
		}

	suspend fun writeTime() =
		write(clocksConfig.homeClock.getCurrentDateTimePacket(delay = 0.seconds))
}

class WatchException (message: String, cause: Throwable) : Exception(message, cause)
