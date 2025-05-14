package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime

@Preview
@Composable
fun TimePickerFieldPreview() {
	var time by remember { mutableStateOf(LocalTime(hour = 10, minute = 10)) }

	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		TimePickerField(
			value = time,
			onValueChange = { time = it },
//			label = "",
			enabled = false,
			modifier = Modifier.fillMaxWidth()
		)
		TimePickerField(
			value = time,
			onValueChange = { time = it },
//			label = "",
			enabled = true,
			modifier = Modifier.fillMaxWidth()
		)
	}
}