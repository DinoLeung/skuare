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
import xyz.d1n0.lib.model.Clock
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
	val pinnedClock = state.pendingClocks.pinnedClock
	val clocks = state.pendingClocks.clocks

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
		modifier = Modifier.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(8.dp),
	) {
		pinnedClock?.let {
			ClockCard(
				modifier = Modifier.fillMaxWidth(),
				clock = it,
				onClick = { selectedClockPosition = 0 },
				enabled = state.isInitialized && state.isLoading == false,
			)
		}
		clocks.forEachIndexed { index, clock ->
			clock?.let {
				ClockCard(
					modifier = Modifier.fillMaxWidth(),
					clock = it,
					onClick = { selectedClockPosition = index + 1 },
					enabled = state.isInitialized && state.isLoading == false,
					onPin = { onEvent(ClocksUiEvent.PinClock(index)) },
				)
			}
		}
	}

	selectedClockPosition?.let { position ->
		val clock = if (position == 0) pinnedClock else clocks.getOrNull(position - 1)
		clock?.let {
			ClockTimeZonePickerDialog(
				clock = it,
				onConfirm = { newClock ->
					onEvent(
						if (position == 0) ClocksUiEvent.PinnedClockChange(newClock)
						else ClocksUiEvent.ClockChange(index = position - 1, clock = newClock)
					)
					selectedClockPosition = null
				},
				onDismiss = { selectedClockPosition = null },
			)
		}
	}
}

/**
 * Opens the time zone picker for [clock], listing time zones from the table that matches its type,
 * so a swapped clock keeps choosing from its own table whichever slot it is in.
 */
@Composable
private fun ClockTimeZonePickerDialog(
	clock: Clock,
	onConfirm: (Clock) -> Unit,
	onDismiss: () -> Unit,
) = when (clock) {
	is HomeClock -> TimeZonePickerDialog(
		timeZone = clock.timeZone,
		timeZones = HomeTimezoneData.values,
		visible = true,
		onConfirm = { onConfirm(clock.copy(timeZone = it)) },
		onDismiss = onDismiss,
		title = "Select home time zone",
	)

	is WorldClock -> TimeZonePickerDialog(
		timeZone = clock.timeZone,
		timeZones = WorldTimezoneData.values,
		visible = true,
		onConfirm = { onConfirm(clock.copy(timeZone = it)) },
		onDismiss = onDismiss,
		title = "Select world time zone",
	)
}

@Preview(showBackground = true)
@Composable
private fun ClocksScreenPreview() {
	val clocks = ClocksSettings(
		pinnedClock = HomeClock.fromTimeZoneId(
			timeZoneId = 30335,
			dstSettings = DstSettings(enable = false, auto = true),
		),
		clocks = listOf(
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

@Preview(showBackground = true)
@Composable
private fun ClocksScreenSwappedPreview() {
	val clocks = ClocksSettings(
		pinnedClock = WorldClock.fromTimeZoneId(
			timeZoneId = 122,
			dstSettings = DstSettings(enable = false, auto = true),
		),
		clocks = listOf(
			HomeClock.fromTimeZoneId(
				timeZoneId = 30335,
				dstSettings = DstSettings(enable = false, auto = true),
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
