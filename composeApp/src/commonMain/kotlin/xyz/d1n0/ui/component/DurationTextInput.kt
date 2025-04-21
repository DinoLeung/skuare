package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.helper.fromHHMMSS
import xyz.d1n0.lib.helper.toHHMMSSString
import kotlin.time.Duration

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DurationTextInput (
    duration: Duration,
    onDurationChange: (Duration) -> Unit
) {
    val mask = "##:##:##"
    val transformation = MaskVisualTransformation(mask)

    OutlinedTextField(
        value = duration.toHHMMSSString(),
        onValueChange = { input -> onDurationChange(Duration.fromHHMMSS(input)) },
        label = { Text("Duration") },
        placeholder = { Text("00:00:00") },
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        isError = duration.toHHMMSSString().length > 6,
        supportingText = {
            if (duration.toHHMMSSString().length > 6)
                Text("Max 6 digits (HHMMSS)")
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

class MaskVisualTransformation(private val mask: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.padStart(mask.count { it=='#' }, '0')
        val out = StringBuilder()
        var di = 0
        mask.forEach { m ->
            if (m == '#')
                out.append(digits[di++])
            else
                out.append(m)
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) =
                (0 until mask.length).count {
                    mask[it] != '#' || it < offset + (mask.take(it).count { it!='#' })
                }
            override fun transformedToOriginal(offset: Int) =
                mask.take(offset).count { it == '#' }
        }
        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}