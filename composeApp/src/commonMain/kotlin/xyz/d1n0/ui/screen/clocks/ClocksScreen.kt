package xyz.d1n0.ui.screen.clocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.Log

@Composable
fun ClocksScreen(
    innerPadding: PaddingValues,
) {
    val viewModel = koinViewModel<ClocksScreenViewModel>()
    val log = koinInject<Log>()

    val watchConnectionState = viewModel.watch.state.collectAsState()
    val isClocksInitialized = viewModel.watch.clocks.isInitialized.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (isClocksInitialized.value == false)
            viewModel.requestClocks()
    }

    Column (
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

    ) {
        if (isClocksInitialized.value == true) {
            Text(viewModel.watch.clocks.homeClock.timeZone.cityName, color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.watch.clocks.worldClock1.timeZone.cityName, color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.watch.clocks.worldClock2.timeZone.cityName, color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.watch.clocks.worldClock3.timeZone.cityName, color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.watch.clocks.worldClock4.timeZone.cityName, color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.watch.clocks.worldClock5.timeZone.cityName, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}