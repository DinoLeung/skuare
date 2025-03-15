package xyz.d1n0.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.model.Watch
import xyz.d1n0.viewModel.ScanScreenViewModel
import xyz.d1n0.viewModel.WatchScreenViewModel

@Composable
@Preview
fun WatchScreen(
    navBack: () -> Unit
) {
    val viewModel = koinViewModel<WatchScreenViewModel>()

    LaunchedEffect(Unit) {
        viewModel.connect(onConnectionLost = navBack)
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Button(onClick = viewModel::getName) {
                Text("Get Name")
            }
            Button(onClick = viewModel::syncTime) {
                Text("Sync Time")
            }
            Button(onClick = { viewModel.disconnect(onDisconnected = navBack) }) {
                Text("Disconnect")
            }
        }
    }
}