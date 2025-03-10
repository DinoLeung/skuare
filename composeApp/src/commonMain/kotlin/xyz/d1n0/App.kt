package xyz.d1n0

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.juul.kable.Peripheral
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import skuare.composeapp.generated.resources.Res
import skuare.composeapp.generated.resources.compose_multiplatform
import xyz.d1n0.constant.Command
import xyz.d1n0.model.Watch
import kotlin.time.Duration.Companion.seconds

@Composable
@Preview
fun App() {

	MaterialTheme {

		val coroutineScope = rememberCoroutineScope()
		var watch by remember { mutableStateOf<Watch?>(null) }

		var scanJob by remember { mutableStateOf<Job?>(null) }
		var isScanning by remember { mutableStateOf(false) }
		var isConnected by remember { mutableStateOf(false) }

		fun startScanning() {
			scanJob = coroutineScope.launch {
				isScanning = true
				Watch.scanner.advertisements.firstOrNull()?.let {
					watch = Watch(Peripheral(it))
					watch?.connect().also {
						isConnected = true
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
					watch = null
					isConnected = false
				}
			}
		}

		fun getName() {
			watch?.let {
				it.scope.launch {
					it.requestName()
				}
			}
		}

		fun syncTime() {
			watch?.let {
				coroutineScope.launch {
					it.requestClocks().join()
					delay(2.seconds)
					it.writeClocks().join()
					it.writeTimeZoneConfigs().join()
					it.writeTimeZoneNames().join()
					it.writeTime().join()
				}
			}
		}

		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center) {

            Button(onClick = { startScanning() }, enabled = !isScanning && !isConnected) {
                Text("Scan!")
            }

			Button(onClick = { stopScanning() }, enabled = isScanning) { Text("Stop!") }

			Button(onClick = { disconnect() }, enabled = isConnected) { Text("Disconnect") }

			Button(onClick = { getName() }, enabled = isConnected) { Text("Get Name!") }

//			Button(onClick = { syncTime() }, enabled = isConnected) { Text("Sync Time!") }

		}
	}
}

suspend fun scan(onWatchFound: suspend (Watch) -> Unit) {
	Watch.scanner.advertisements
		.firstOrNull()
		?.let { onWatchFound(Watch(Peripheral(it))) }
}