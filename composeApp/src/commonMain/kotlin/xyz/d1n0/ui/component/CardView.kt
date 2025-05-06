package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun CardView(
	leadingIcon: @Composable (() -> Unit)? = null,
	title: @Composable (() -> Unit)? = null,
	indicator: @Composable (() -> Unit)? = null,
	content: @Composable (() -> Unit)? = null,
	modifier: Modifier = Modifier,
) {
	Card(
		modifier = modifier.padding(8.dp),
		shape = MaterialTheme.shapes.large,
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.align(Alignment.Start).fillMaxWidth(),
			) {
				leadingIcon?.let { Box { it() } }
				title?.let { Box(modifier = Modifier.weight(1f)) { it() } }
				indicator?.let { Box { it() } }
			}
			content?.let { it() }
		}
	}
}
