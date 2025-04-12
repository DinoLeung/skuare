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

    val isInitialized = viewModel.isInitialized.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (isInitialized.value == false)
            viewModel.requestClocks()
    }

    Column (
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

    ) {
        if (isInitialized.value == true) {
            Text(viewModel.clocks.homeClock.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.clocks.worldClock1.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.clocks.worldClock2.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.clocks.worldClock3.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.clocks.worldClock4.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
            Text(viewModel.clocks.worldClock5.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
        }
    }
}