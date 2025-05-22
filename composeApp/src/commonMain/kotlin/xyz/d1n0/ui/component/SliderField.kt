package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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