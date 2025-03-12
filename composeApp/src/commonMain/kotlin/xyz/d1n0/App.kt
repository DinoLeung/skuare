package xyz.d1n0

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.juul.kable.Peripheral
import com.juul.kable.State
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.ui.tooling.preview.Preview

import xyz.d1n0.model.Watch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
@Preview
fun App() {

	MaterialTheme {

		val coroutineScope = rememberCoroutineScope()
		var watch by remember { mutableStateOf<Watch?>(null) }

		var scanJob by remember { mutableStateOf<Job?>(null) }

		// TODO: should there be a better way to track scanning status?
		var isScanning by remember { mutableStateOf(false) }

		fun startScanning() {
			scanJob = coroutineScope.launch {
				isScanning = true
				Watch.scanner.advertisements.firstOrNull()?.let {
					watch = Watch(Peripheral(it))
					watch?.connect().also {
						println("Watch connected")
					}
				}
				isScanning = false
			}
		}

		fun stopScanning() {
			scanJob?.cancel().also {
				isScanning = false
			}
		}

		fun disconnect() {
			coroutineScope.launch {
				watch?.disconnect().also {
				}
			}
		}

		fun getName() {
			println("getting name")
			watch?.let {
				it.scope.launch { it.requestName() }
			}
		}

		fun syncTime() = watch?.let {
			it.scope.launch {
				runCatching {
					it.requestClocks()
					withTimeoutOrNull(10.seconds) {
						while (!it.clocksConfig.hasInitializedClocks()) {
							delay(100.milliseconds)
						}
					} ?: throw IllegalStateException("Timeout waiting for clocks initialization")
					it.writeClocks()
					it.writeTimeZoneConfigs()
					it.writeTimeZoneNames()
					it.writeTime()
				}.onSuccess {
					println("Time sync completed")
				}.onFailure { error ->
					println("Error syncing time: ${error.message}")
				}
			}
		} ?: throw IllegalStateException("Watch is not connected")

		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center) {

            Button(onClick = { startScanning() }, enabled = !isScanning && watch?.connectionState !is State.Connected) {
                Text("Scan!")
            }

			Button(onClick = { stopScanning() }, enabled = isScanning) { Text("Stop!") }

			Button(onClick = { disconnect() }, enabled = watch?.connectionState is State.Connected) { Text("Disconnect") }

			Button(onClick = { getName() }, enabled = watch?.connectionState is State.Connected) { Text("Get Name!") }

			Button(onClick = { syncTime() }, enabled = watch?.connectionState is State.Connected) { Text("Sync Time!") }
		}
	}
}

suspend fun scan(onWatchFound: suspend (Watch) -> Unit) {
	Watch.scanner.advertisements
		.firstOrNull()
		?.let { onWatchFound(Watch(Peripheral(it))) }
}