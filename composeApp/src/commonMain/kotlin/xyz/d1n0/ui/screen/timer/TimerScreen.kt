package xyz.d1n0.ui.screen.timer

import androidx.compose.foundation.layout.*
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
fun TimerScreen(
    innerPadding: PaddingValues,
) {
    val viewModel = koinViewModel<TimerScreenViewModel>()
    val log = koinInject<Log>()

    val isInitialized = viewModel.isInitialized.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (isInitialized.value == false)
            viewModel.requestTimer()
    }

    Column (
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {
        if (isInitialized.value == true) {
            Text(viewModel.timer.value.toString())
        }
    }
}