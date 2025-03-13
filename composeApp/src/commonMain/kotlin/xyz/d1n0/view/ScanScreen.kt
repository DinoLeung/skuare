package xyz.d1n0.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.viewModel.ScanScreenViewModel

@Composable
@Preview
fun ScanScreen(
    viewModel: ScanScreenViewModel = viewModel { ScanScreenViewModel() }
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = viewModel::startScanning , enabled = !state.isScanning) {
            Text("Scan!")
        }
        Button(onClick = viewModel::stopScanning , enabled = state.isScanning) {
            Text("Stop!")
        }
    }
}