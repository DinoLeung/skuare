package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.helper.fromHHMMSS
import xyz.d1n0.lib.helper.toHHMMSSString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DurationTextInput(
	duration: Duration,
	onDurationChange: (Duration) -> Unit,
	label: @Composable (() -> Unit)? = null,
	placeholder: @Composable () -> Unit = { Text("00h 00m 00s") },
	supportingText: @Composable (() -> Unit)? = null,
	enabled: Boolean = true,
	isError: Boolean = false,
	modifier: Modifier = Modifier,
) {
	var textFieldValue by remember {
		mutableStateOf(
			TextFieldValue(
				text = duration.toHHMMSSString(),
				selection = TextRange(duration.toHHMMSSString().length)
			)
		)
	}

	LaunchedEffect(duration) {
		textFieldValue = textFieldValue.copy(
			text = duration.toHHMMSSString(),
			selection = TextRange(duration.toHHMMSSString().length)
		)
		onDurationChange(duration)
	}

	OutlinedTextField(
		value = textFieldValue,
		onValueChange = { input ->
			val raw =
				input.text.filter { it.isDigit() }.trimStart { it == '0' }.take(6).padStart(6, '0')
			val newDuration = Duration.fromHHMMSS(raw)
			textFieldValue = input.copy(
				text = raw,
				selection = TextRange(raw.length),
			)
			onDurationChange(newDuration)
		},
		label = label,
		placeholder = placeholder,
		visualTransformation = MaskVisualTransformation(),
		keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
		enabled = enabled,
		isError = isError,
		supportingText = supportingText,
		singleLine = true,
		modifier = modifier
	)
}

private class MaskVisualTransformation : VisualTransformation {
	private val mask = "##h ##m ##s"
	override fun filter(text: AnnotatedString): TransformedText {
		val digits = text.text.padStart(mask.count { it == '#' }, '0')
		val formatted = StringBuilder()
		var digitIndex = 0
		mask.forEach { m ->
			if (m == '#') formatted.append(digits[digitIndex++])
			else formatted.append(m)
		}
		val offsetMapping = object : OffsetMapping {
			override fun originalToTransformed(offset: Int): Int {
				var transformedIndex = 0
				var digitsSeen = 0
				while (digitsSeen < offset && transformedIndex < mask.length) {
					if (mask[transformedIndex] == '#') {
						digitsSeen++
					}
					transformedIndex++
				}
				return transformedIndex
			}

			override fun transformedToOriginal(offset: Int) = mask.take(offset).count { it == '#' }
		}
		return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
	}
}

@Preview
@Composable
private fun DurationTextInputPreview() {
	var duration by remember { mutableStateOf(69.minutes) }

	Card(modifier = Modifier.padding(8.dp)) {
		DurationTextInput(
			duration = duration,
			onDurationChange = { duration = it },
			modifier = Modifier.padding(8.dp),
			isError = duration > 12.hours,
			supportingText = {
				if (duration > 12.hours) {
					androidx.compose.material.Text("Cannot exceed 12 hours.")
				}
			}
		)
	}
}