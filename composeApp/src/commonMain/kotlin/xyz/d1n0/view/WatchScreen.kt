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
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
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

            Button(onClick = viewModel::getConnectReason) {
                Text("Get Reason")
            }
            Button(onClick = viewModel::getTimeSyncSettings) {
                Text("Get TimeSync Settings")
            }
            Button(onClick = viewModel::getWatchSettings) {
                Text("Get Watch Settings")
            }
            Button(onClick = viewModel::getInfo) {
                Text("Get Info")
            }
            Button(onClick = viewModel::getName) {
                Text("Get Name")
            }
            Button(onClick = viewModel::getWatchCondition) {
                Text("Get Condition")
            }
            Button(onClick = viewModel::getAlarms) {
                Text("Get Alarms")
            }
            Button(onClick = viewModel::writeAlarms) {
                Text("Write Alarms")
            }
            Button(onClick = viewModel::getTimer) {
                Text("Get Timer")
            }
            Button(onClick = viewModel::writeTimer) {
                Text("Write Timer")
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