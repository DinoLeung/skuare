package xyz.d1n0.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_LE
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_SCAN
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.location.BACKGROUND_LOCATION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.viewModel.ScanScreenViewModel

@Composable
@Preview
fun ScanScreen(
    navToWatch: () -> Unit
) {
    val viewModel = koinViewModel<ScanScreenViewModel>()
    val state by viewModel.state.collectAsState()

    val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val controller: PermissionsController = remember(factory) { factory.createPermissionsController() }
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    BindEffect(controller)

    LaunchedEffect(Unit) {
        coroutineScope.launch {
//            controller.providePermission(Permission.BLUETOOTH_LE)
            controller.providePermission(Permission.BLUETOOTH_SCAN)
//            controller.providePermission(Permission.BACKGROUND_LOCATION)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = { viewModel.startScanning(onWatchFound = navToWatch) }, enabled = !state.isScanning) {
            Text("Scan!")
        }
        Button(onClick = viewModel::stopScanning , enabled = state.isScanning) {
            Text("Stop!")
        }
    }
}