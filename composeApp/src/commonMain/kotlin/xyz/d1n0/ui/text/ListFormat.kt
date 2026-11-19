package xyz.d1n0.ui.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.resources.*

/**
 * Joins the items with the language's list separator, e.g. "Alarms, Settings". Empty if there are
 * none.
 */
@Composable
fun List<String>.joinLocalized(): String =
	if (isEmpty()) "" else reduce { joined, item -> stringResource(Res.string.common_list_join, joined, item) }
