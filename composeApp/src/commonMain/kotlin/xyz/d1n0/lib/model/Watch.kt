package xyz.d1n0.lib.model

import com.juul.kable.ExperimentalKableApi
import com.juul.kable.NotConnectedException
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.juul.kable.logs.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.Log
import xyz.d1n0.lib.constant.BleUuid
import xyz.d1n0.lib.constant.OpCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

/**
 * A connected Casio watch.
 *
 * @param peripheral The watch to talk to.
 * @param advertisedName Name the watch advertises, e.g. CASIO GMW-B5000#, null if unknown. It
 *        decides [model], so it is fixed when the watch is created. Not the same as the watch
 *        name from [OpCode.WATCH_NAME], which the user can set to anything.
 */
class Watch @OptIn(ExperimentalKableApi::class) constructor(
	private val peripheral: Peripheral,
	val advertisedName: String? = peripheral.name,
) : KoinComponent {

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
		private val dataRequestCharacteristic = characteristicOf(
			service = BleUuid.ALL_FEATURES_CHARACTERISTIC_UUID,
			characteristic = BleUuid.DATA_REQUEST_CHARACTERISTIC_UUID
		)
		private val dataTransferCharacteristic = characteristicOf(
			service = BleUuid.ALL_FEATURES_CHARACTERISTIC_UUID,
			characteristic = BleUuid.DATA_TRANSFER_CHARACTERISTIC_UUID
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

	/**
	 * What this watch has, from [advertisedName]. Unknown names are treated as
	 * [WatchModel.CLASSIC_SQUARE], which is what skuare supported before there were models.
	 */
	val model: WatchModel = WatchModel.fromAdvertisedName(advertisedName)
		?.also { log.i { "Watch $advertisedName is $it" } }
		?: WatchModel.CLASSIC_SQUARE.also { log.w { "Unknown watch $advertisedName, treating it as $it" } }

	internal val _info = MutableStateFlow(WatchInfo())
	val info: StateFlow<WatchInfo> = _info.asStateFlow()

	internal val _clocks = MutableStateFlow(ClocksSettings())
	val clocks: StateFlow<ClocksSettings> get() = _clocks.asStateFlow()

	/** Last clocks packet parse failure since clocks were requested, so waits can fail fast. */
	internal val _clocksError = MutableStateFlow<Throwable?>(null)

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

	private val _packets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

	/**
	 * Packets received on the IO characteristic, emitted after [handlePacket] has processed them.
	 */
	val packets: SharedFlow<ByteArray> = _packets.asSharedFlow()

	/**
	 * A coroutine Job that continuously observes the peripheral’s IO characteristic.
	 *
	 * This job is launched in the [peripheral]'s coroutine scope and collects data packets emitted by the IO characteristic.
	 * Each received packet is processed by the [handlePacket] function, then emitted to [packets].
	 * The job will automatically be cancelled when the peripheral's scope is cancelled.
	 */
	val ioDataObserverJob = peripheral.scope.launch {
		peripheral.observe(ioCharacteristic).collect {
			handlePacket(it)
			_packets.emit(it)
		}
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
	 * Closes the peripheral and cancels [scope], the watch can't be connected again afterwards.
	 */
	fun close() = peripheral.close()

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
	 * Sends a request and waits for the reply, for when the caller needs the reply before it can
	 * continue. The reply is also processed by [handlePacket] as usual.
	 *
	 * @param opCode The command to be sent to the peripheral.
	 * @param timeout How long to wait for the reply.
	 * @return The first packet received with the same opcode.
	 * @throws kotlinx.coroutines.TimeoutCancellationException if no reply arrives in time, e.g. the
	 *         watch replied with an error instead.
	 */
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun query(opCode: OpCode, timeout: Duration = 5.seconds): ByteArray =
		withTimeout(timeout) {
			coroutineScope {
				// subscribe before requesting, so the reply can't arrive before we listen
				val reply = async(start = CoroutineStart.UNDISPATCHED) {
					packets.first { it.firstOrNull() == opCode.byte }
				}
				request(opCode)
				reply.await()
			}
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

	/**
	 * Requests data from the data request characteristic and waits for the reply on the data
	 * transfer characteristic, the same way the official app does:
	 * 1. Enables notifications on the data transfer characteristic.
	 * 2. Writes [request] to the data request characteristic.
	 * 3. Waits for one notification on the data transfer characteristic.
	 * 4. Writes [end] to the data request characteristic.
	 * 5. Disables notifications on the data transfer characteristic.
	 *
	 * @param request The packet that starts the transfer, e.g. [WatchStatus.requestPacket].
	 * @param end The packet that ends the transfer, e.g. [WatchStatus.endPacket].
	 * @param timeout How long to wait for the reply.
	 * @return The received data.
	 */
	@OptIn(ExperimentalStdlibApi::class)
	@Throws(CancellationException::class, IOException::class, NotConnectedException::class)
	suspend fun requestData(request: ByteArray, end: ByteArray, timeout: Duration = 5.seconds): ByteArray {
		log.d { "Requesting data: ${request.toHexString(HexFormat.UpperCase)}" }
		return withTimeout(timeout) {
			peripheral.observe(dataTransferCharacteristic) {
				peripheral.write(dataRequestCharacteristic, request, WriteType.WithResponse)
			}
				// write end packet before first() cancels the observation, which disables notifications
				.onEach { peripheral.write(dataRequestCharacteristic, end, WriteType.WithResponse) }
				.first()
		}.also { log.d { "Received data: ${it.toHexString(HexFormat.UpperCase)}" } }
	}
}

//class WatchException (message: String, cause: Throwable) : Exception(message, cause)
