package xyz.d1n0

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.juul.kable.Peripheral
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import skuare.composeapp.generated.resources.Res
import skuare.composeapp.generated.resources.compose_multiplatform
import xyz.d1n0.model.Watch

@Composable
@Preview
fun App() {

//	val scan = {
//		Watch.scanner.advertisements
//			.collect {
//				Peripheral(it.)
//			}
//	}

	MaterialTheme {
		var showContent by remember { mutableStateOf(false) }

		Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
			Button(onClick = { showContent = !showContent }) {
				Text("Click me!")
			}
//            Button(onClick = scan) {
//                Text("Scan!")
//            }
			AnimatedVisibility(showContent) {
				val greeting = remember { Greeting().greet() }
				Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
					Image(painterResource(Res.drawable.compose_multiplatform), null)
					Text("Compose: $greeting")
				}
			}
		}
	}
}
