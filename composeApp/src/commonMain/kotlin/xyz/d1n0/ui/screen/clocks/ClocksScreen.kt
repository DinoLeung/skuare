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

@Composable
fun ClocksScreen(
    innerPadding: PaddingValues,
) {
    val viewModel = koinViewModel<ClocksScreenViewModel>()
    val isInitialized = viewModel.isInitialized.collectAsState(initial = false)
    val homeClock = viewModel.homeClock.collectAsState(null)
    val worldClocks = viewModel.worldClocks.collectAsState(List(5) { null })

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
            homeClock.value?.let {
                Text(it.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
            }
            worldClocks.value.map { clock ->
                clock?.let {
                    Text(it.timeZone.toString(), color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}