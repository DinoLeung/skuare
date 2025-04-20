package xyz.d1n0.ui.screen.clocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.ui.component.Clock

@Preview
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        homeClock.value?.let {
            Clock(
                modifier = Modifier.fillMaxWidth(),
                clock = it,
            )
        }
        worldClocks.value.map { clock ->
            clock?.let {
                Clock(
                    modifier = Modifier.fillMaxWidth(),
                    clock = it
                )
            }
        }
    }
}