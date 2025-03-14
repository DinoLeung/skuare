package xyz.d1n0.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.model.Watch
import xyz.d1n0.viewModel.WatchScreenViewModel

@Serializable
data class WatchRoute(val watch: Watch)

@Composable
@Preview
fun WatchScreen(
    navController: NavHostController,
    watch: Watch,
    viewModel: WatchScreenViewModel = viewModel { WatchScreenViewModel(navController, watch) },
) {
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
            Button(onClick = viewModel::disconnect) {
                Text("Disconnect")
            }
        }
    }
}