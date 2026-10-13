package xyz.d1n0.ui.screen.clocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import xyz.d1n0.lib.model.DstSettings
import xyz.d1n0.lib.model.HomeClock
import xyz.d1n0.lib.model.WorldClock
import xyz.d1n0.ui.component.ClockCard

@Composable
fun ClocksScreen() {
	val viewModel = koinViewModel<ClocksScreenViewModel>()
	val isInitialized = viewModel.isInitialized.collectAsState(initial = false)
	val homeClock = viewModel.homeClock.collectAsState(null)
	val worldClocks = viewModel.worldClocks.collectAsState(List(5) { null })

	LaunchedEffect(Unit) {
		if (isInitialized.value == false) viewModel.requestClocks()
	}

	ClocksScreenContent(
		homeClock = homeClock.value,
		worldClocks = worldClocks.value,
	)
}

@Composable
private fun ClocksScreenContent(
	homeClock: HomeClock?,
	worldClocks: List<WorldClock?>,
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
		modifier = Modifier.fillMaxSize()
			.verticalScroll(rememberScrollState()),
	) {
		homeClock?.let {
			ClockCard(
				modifier = Modifier.fillMaxWidth(),
				clock = it,
			)
		}
		worldClocks.forEach { clock ->
			clock?.let {
				ClockCard(
					modifier = Modifier.fillMaxWidth(), clock = it
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun ClocksScreenPreview() {
	MaterialTheme {
		ClocksScreenContent(
			homeClock = HomeClock.fromTimeZoneId(
				timeZoneId = 30335,
				dstSettings = DstSettings(enable = false, auto = true),
			),
			worldClocks = listOf(
				WorldClock.fromTimeZoneId(
					timeZoneId = 95,
					dstSettings = DstSettings(enable = true, auto = false),
				),
			),
		)
	}
}
