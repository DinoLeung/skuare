package xyz.d1n0.screen.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.icerock.moko.permissions.compose.BindEffect
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun ScanScreen(
    navToWatch: () -> Unit
) {
    val viewModel = koinViewModel<ScanScreenViewModel>()
    val state by viewModel.state.collectAsState()
    val scope: CoroutineScope = rememberCoroutineScope()

    BindEffect(viewModel.permissionsController)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkScanPermissions()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.hasScanPermission) {
            Button(
                onClick = { viewModel.startScanning(onWatchFound = navToWatch) },
                enabled = !state.isScanning,
            ) {
                Text("Scan!")
            }
            Button(
                onClick = viewModel::stopScanning,
                enabled = state.isScanning,
            ) {
                Text("Stop!")
            }
        } else {
            Button(
                onClick = viewModel.permissionsController::openAppSettings,
            ) {
                Text("Grant permissions from app settings")
            }
        }
    }
}