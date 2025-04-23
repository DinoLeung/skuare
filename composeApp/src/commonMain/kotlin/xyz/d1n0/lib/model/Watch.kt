package xyz.d1n0.lib.model

import com.juul.kable.NotConnectedException
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.constant.BleUuid
import xyz.d1n0.lib.constant.OpCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi

class Watch(private val peripheral: Peripheral) : KoinComponent {

	@OptIn(ExperimentalUuidApi::class)
	companion object {
		private val requestCharacteristic = characteristicOf(
			service = BleUuid.ALL_FEATURES_CHARACTERISTIC_UUID,
			characteristic = BleUuid.REQUEST_CHARACTERISTIC_UUID
		)
		private val ioCharacteristic = characteristicOf(
			service = BleUuid.ALL_FEATURES_CHARACTERISTIC_UUID,
			characteristic = BleUuid.IO_CHARACTERISTIC_UUID
		)

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

	val log: Log by inject()

	internal val _info = MutableStateFlow(WatchInfo())
	val info: StateFlow<WatchInfo> = _info.asStateFlow()

	internal val _clocks = MutableStateFlow(ClocksSettings())
	val clocks: StateFlow<ClocksSettings> get() = _clocks.asStateFlow()

	internal val _alarms = MutableStateFlow(AlarmsSettings())
	val alarms: StateFlow<AlarmsSettings> get() = _alarms.asStateFlow()

	internal val _timer = MutableStateFlow(TimerSettings())
	val timer: StateFlow<TimerSettings> get() = _timer.asStateFlow()

	internal val _reminders = MutableStateFlow(RemindersSettings())
	val reminders: StateFlow<RemindersSettings> = _reminders.asStateFlow()

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
	 * A coroutine Job that continuously observes the peripheral’s IO characteristic.
	 *
	 * This job is launched in the [peripheral]'s coroutine scope and collects data packets emitted by the IO characteristic.
	 * Each received packet is processed by the [handlePacket] function.
	 * The job will automatically be cancelled when the peripheral's scope is cancelled.
	 */
	val ioDataObserverJob = peripheral.scope.launch {
		peripheral.observe(ioCharacteristic).collect { handlePacket(it) }
	}

	/**
	 * Connects to q peripheral watch.
	 */
	suspend fun connect() = peripheral.connect()

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
	@OptIn(ExperimentalStdlibApi::class)
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun request(opCode: OpCode) {
		log.d { "Requesting: ${opCode.byte.toHexString(HexFormat.UpperCase)}" }
		runCatching {
			peripheral.write(
				requestCharacteristic,
				byteArrayOf(opCode.byte)
			)
		}.onFailure { log.e { "Failed to request ${opCode.name}: ${it.message}" } }

	}

	/**
	 * Sends a request to the peripheral with a specified command and position.
	 * Results will be returned through the IO characteristic.
	 *
	 * @param opCode The command to be sent to the peripheral.
	 * @param position The position value accompanying the command.
	 * @return A [Unit] that completes when the command and position are written successfully.
	 */
	@OptIn(ExperimentalStdlibApi::class)
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun request(opCode: OpCode, position: Int) {
		log.d { "Requesting: ${opCode.byte.toHexString(HexFormat.UpperCase)} position: $position" }
		runCatching {
			peripheral.write(
				requestCharacteristic, byteArrayOf(opCode.byte, position.toByte())
			)
		}.onFailure { log.e { "Failed to request ${opCode.name} on $position: ${it.message}" } }
	}

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
		log.d { "Writing: ${data.toHexString(HexFormat.UpperCase)}" }
		runCatching {
			peripheral.write(
				ioCharacteristic,
				data,
				WriteType.WithResponse
			)
		}.onFailure { log.e { "Failed to write ${data.toHexString(HexFormat.UpperCase)}: ${it.message}" } }

	}
}

//class WatchException (message: String, cause: Throwable) : Exception(message, cause)
