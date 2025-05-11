package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.constant.AutoSyncDelay

@Preview
@Composable
fun SliderPreview() {

	var delay by remember { mutableStateOf(AutoSyncDelay(30)) }
	var error: Throwable? by remember { mutableStateOf(null) }

	fun update(value: Int) {
		runCatching {
			AutoSyncDelay(value)
		}.fold(
			onSuccess = {
				delay = it
				error = null
			},
			onFailure = { error = it },
		)
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.safeContentPadding(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		CardView(modifier = Modifier.fillMaxWidth()) {
			SliderField(
				modifier = Modifier.fillMaxWidth(),
				label = "test",
				enabled = true,
				value = delay.minutes,
				range = 0..59,
				onValueChange = {
					update(it)
				},
				error = error,
			)
		}
	}
}