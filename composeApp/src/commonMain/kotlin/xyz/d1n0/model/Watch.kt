package xyz.d1n0.model

import com.juul.kable.*
import com.juul.kable.logs.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import xyz.d1n0.constant.BleUuid
import xyz.d1n0.constant.Command
import xyz.d1n0.constant.ConnectReason
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
	val watchConfig = WatchInfo()
	// TODO: Alarm 1, 2, 3, 4, snooze
	// TODO: hourly signal
	// TODO: Stopwatch?
	// TODO: Timer
	// TODO: Reminder

	val scope: CoroutineScope get() = peripheral.scope
	val state: StateFlow<State> get() = peripheral.state

	private val ioCharacteristicObservation = peripheral.observe(ioCharacteristic)

	suspend fun connect() = peripheral.connect()
		.launch { observeIoCharacteristic() }

	suspend fun disconnect() = peripheral.disconnect()

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
				Command.CONNECT_REASON -> {
					val reason = ConnectReason.fromValue(it.get(8).toInt())
					when (reason) {
						ConnectReason.SETUP, ConnectReason.DEFAULT -> {
							// TODO: Check APP_INFO, up date if required
							// TODO:
						}
						ConnectReason.AUTO_SYNC, ConnectReason.MANUAL_SYNC -> {
							// TODO: sync time procedure, then disconnect
							// TODO: figure out how long the watch will stay connected for
						}
						ConnectReason.FIND -> {
							// TODO: figure out if sync time is required
							// TODO: figure out how long the watch will stay connected for
						}
					}

				}
				Command.CONNECTION_SETTINGS -> {
					runCatching {
						watchConfig.parseConnectionSettingsPacket(it)
					}.onFailure { println("Failed to parse auto sync settings packet: ${it.message}") }
				}
				Command.APP_INFO -> {
					// new/reset watch		22FFFFFFFFFFFFFFFFFFFF00
					// b5600 old app ver.	222DA85E248C468C74834202
					// b5600 newer app ver.	228C8973A1B416502E67DD02
					// Looks like the app will compare this number to determine if the watch has been paired or not
					// probably can set to anything
				}
				Command.WATCH_SETTINGS -> {
					runCatching {
						watchConfig.parseWatchSettingsPacket(it)
					}.onFailure { println("Failed to parse settings packet: ${it.message}") }
				}
				Command.WATCH_NAME -> {
					runCatching {
						watchConfig.parseNamePacket(it)
					}.onFailure { println("Failed to parse name packet: ${it.message}") }
				}
				Command.WATCH_CONDITION -> {
					// 28 13 1F 00
				}
				Command.CLOCK -> {
					runCatching {
						clocksConfig.parseClocksPacket(it)
					}.onFailure { println("Failed to parse clocks packet: ${it.message}") }
				}

				Command.ERROR -> {
					println("Error: ${it.toHexString(HexFormat.UpperCase)}")
				}
				// TODO: implement other commands
				else -> {
					println("Unsupported packet: ${it.toHexString(HexFormat.UpperCase)}")
				}
			}
		}

	suspend fun requestConnectReason() = request(Command.CONNECT_REASON)
	suspend fun requestTimeSyncSettings() = request(Command.CONNECTION_SETTINGS)
	suspend fun requestWatchSettings() = request(Command.WATCH_SETTINGS)
	suspend fun requestAppInfo() = request(Command.APP_INFO)
	suspend fun requestName() = request(Command.WATCH_NAME)
	suspend fun requestWatchCondition() = request(Command.WATCH_CONDITION)

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
