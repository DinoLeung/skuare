package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.AutoSyncDelay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderField(
	modifier: Modifier = Modifier,
	label: String,
	enabled: Boolean = true,
	value: Int,
	range: IntRange,
	onValueChange: (Int) -> Unit,
	error: Throwable?,
) {
	var textFieldValue by remember {
		mutableStateOf(
			TextFieldValue(
				text = value.toString(),
				selection = TextRange(value.toString().length)
			)
		)
	}

	LaunchedEffect(value) {
		textFieldValue =
			TextFieldValue(text = value.toString(), selection = TextRange(value.toString().length))
	}

	Column(
		modifier = Modifier.fillMaxWidth(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Text(
				text = label,
				style = MaterialTheme.typography.bodyMedium,
				modifier = modifier.weight(5f),
			)
			OutlinedTextField(
				value = textFieldValue,
				onValueChange = {
					textFieldValue = it.copy(selection = TextRange(it.text.toString().length))
					if (it.text.isNotEmpty())
						onValueChange(it.text.toInt())
				},
				keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
				isError = error != null,
				modifier = modifier.weight(1f),
			)
		}
		Slider(
			enabled = enabled,
			value = value.toFloat(),
			steps = range.count(),
			valueRange = range.first.toFloat()..range.last.toFloat(),
			onValueChange = { onValueChange(it.toInt()) },
			modifier = modifier,
		)
		error?.message?.let { ErrorText(text = it) }
	}

}

@Preview
@Composable
private fun SliderPreview() {

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