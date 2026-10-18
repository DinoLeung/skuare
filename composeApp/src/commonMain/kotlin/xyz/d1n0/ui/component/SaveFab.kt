package xyz.d1n0.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.ui.theme.SkuareTheme

@Composable
fun SaveFab(
	modifier: Modifier = Modifier,
	visible: Boolean,
	onClick: () -> Unit,
) {
	AnimatedVisibility(
		visible = visible,
		enter = scaleIn(),
		exit = scaleOut(),
		modifier = modifier,
	) {
		FloatingActionButton(
			onClick = onClick,
		) {
			Icon(
				imageVector = Icons.Outlined.Save,
				contentDescription = "Save",
			)
		}
	}
}

@Preview
@Composable
private fun SaveFabPreview() {
	SkuareTheme {
		SaveFab(
			visible = true,
			onClick = {},
		)
	}
}
