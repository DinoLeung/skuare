package xyz.d1n0.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.OutlinedSecureTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SaveScreenScaffold(
	modifier: Modifier = Modifier,
	saveFabVisible: Boolean,
	saveFabOnClick: () -> Unit,
	content: @Composable () -> Unit,
) {
	Box(modifier = modifier.fillMaxSize()) {
		content()
		AnimatedVisibility(
			visible = saveFabVisible,
			enter = slideInVertically { it },
			exit = slideOutVertically { it },
			modifier = Modifier
				.align(Alignment.BottomEnd)
				.padding(16.dp)
		) {
			FloatingActionButton(
				onClick = saveFabOnClick,
			) {
				Icon(
					imageVector = Icons.Outlined.Save,
					contentDescription = "Save",
				)
			}
		}
	}
}

@Preview
@Composable
private fun SaveScreenScaffoldPreview() {
	SaveScreenScaffold(
		saveFabVisible = true,
		saveFabOnClick = {},
	) {
		Column(
			modifier = Modifier.fillMaxSize().background(Color.Cyan),
			verticalArrangement = Arrangement.SpaceBetween
		) {
			OutlinedSecureTextField(
				state = rememberTextFieldState(),
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp),
			)

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(18.dp)
					.background(color = Color.Green)
			)
		}
	}
}