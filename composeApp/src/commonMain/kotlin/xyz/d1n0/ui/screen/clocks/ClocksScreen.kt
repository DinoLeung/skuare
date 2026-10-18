package xyz.d1n0.ui.screen.clocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.constant.HomeTimezoneData
import xyz.d1n0.lib.constant.WorldTimezoneData
import xyz.d1n0.lib.model.ClocksSettings
import xyz.d1n0.lib.model.DstSettings
import xyz.d1n0.lib.model.HomeClock
import xyz.d1n0.lib.model.WorldClock
import xyz.d1n0.ui.component.ClockCard
import xyz.d1n0.ui.component.TimeZonePickerDialog
import xyz.d1n0.ui.theme.SkuareTheme

@Composable
fun ClocksScreen(viewModel: ClocksScreenViewModel) {
	val state by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		if (state.isInitialized == false) viewModel.onEvent(ClocksUiEvent.RequestClocks)
	}

	ClocksScreenContent(
		state = state,
		onEvent = { viewModel.onEvent(it) },
	)
}

@Composable
private fun ClocksScreenContent(
	state: ClocksUiState,
	onEvent: (ClocksUiEvent) -> Unit,
) {
	var selectedClockPosition by remember { mutableStateOf<Int?>(null) }
	val homeClock = state.pendingClocks.homeClock
	val worldClocks = state.pendingClocks.worldClocks

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
		modifier = Modifier.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(8.dp),
	) {
		homeClock?.let {
			ClockCard(
				modifier = Modifier.fillMaxWidth(),
				clock = it,
				onClick = { selectedClockPosition = 0 },
				enabled = state.isInitialized && state.isLoading == false,
			)
		}
		worldClocks.forEachIndexed { index, clock ->
			clock?.let {
				ClockCard(
					modifier = Modifier.fillMaxWidth(),
					clock = it,
					onClick = { selectedClockPosition = index + 1 },
					enabled = state.isInitialized && state.isLoading == false,
				)
			}
		}
	}

	selectedClockPosition?.let { position ->
		if (position == 0) {
			homeClock?.let { clock ->
				TimeZonePickerDialog(
					timeZone = clock.timeZone,
					timeZones = HomeTimezoneData.values,
					visible = true,
					onConfirm = { timeZone ->
						onEvent(ClocksUiEvent.HomeTimeZoneChange(timeZone))
						selectedClockPosition = null
					},
					onDismiss = { selectedClockPosition = null },
					title = "Select home time zone",
				)
			}
		} else {
			val worldClockIndex = position - 1
			worldClocks.getOrNull(worldClockIndex)?.let { clock ->
				TimeZonePickerDialog(
					timeZone = clock.timeZone,
					timeZones = WorldTimezoneData.values,
					visible = true,
					onConfirm = { timeZone ->
						onEvent(
							ClocksUiEvent.WorldTimeZoneChange(
								index = worldClockIndex,
								timeZone = timeZone,
							)
						)
						selectedClockPosition = null
					},
					onDismiss = { selectedClockPosition = null },
					title = "Select world time zone",
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun ClocksScreenPreview() {
	val clocks = ClocksSettings(
		homeClock = HomeClock.fromTimeZoneId(
			timeZoneId = 30335,
			dstSettings = DstSettings(enable = false, auto = true),
		),
		worldClocks = listOf(
			WorldClock.fromTimeZoneId(
				timeZoneId = 95,
				dstSettings = DstSettings(enable = true, auto = false),
			),
			WorldClock.fromTimeZoneId(
				timeZoneId = 310,
				dstSettings = DstSettings(enable = true, auto = false),
			),
		),
	)
	SkuareTheme {
		ClocksScreenContent(
			state = ClocksUiState(
				isLoading = false,
				savedClocks = clocks,
				pendingClocks = clocks,
			),
			onEvent = {},
		)
	}
}
