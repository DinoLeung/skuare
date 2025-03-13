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
import xyz.d1n0.view.ScanScreen
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
@Preview
fun App() {

	MaterialTheme {

		ScanScreen()

//		fun startObservingState() = coroutineScope.launch {
//			watch?.state?.collect { state ->
//				println("State: $state")
//			}
//		}



	}
}
