package xyz.d1n0.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_SCAN
import dev.icerock.moko.permissions.compose.BindEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.viewModel.ScanScreenState
import xyz.d1n0.viewModel.ScanScreenViewModel

@Composable
@Preview
fun ScanScreen(
    navToWatch: () -> Unit
) {
    val viewModel = koinViewModel<ScanScreenViewModel>()
    val state by viewModel.state.collectAsState()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    BindEffect(viewModel.permissionsController)

    LaunchedEffect(Unit) {
        coroutineScope.launch {
//            controller.providePermission(Permission.BLUETOOTH_LE)
//            if (viewModel.scanPermissionState.value != PermissionState.Granted)

            runCatching {
                viewModel.permissionsController.providePermission(Permission.BLUETOOTH_SCAN)
            }.onFailure {
                if (it is DeniedAlwaysException) {
                    viewModel.log.e { viewModel.scanPermissionState.value.name }
                }
            }
//            controller.providePermission(Permission.BACKGROUND_LOCATION)
        }
    }



    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (viewModel.scanPermissionState.value != PermissionState.Granted) {
            Text("No permission")
        } else {
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
        }
    }
}