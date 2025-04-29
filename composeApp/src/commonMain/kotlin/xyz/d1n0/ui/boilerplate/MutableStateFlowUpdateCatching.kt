package xyz.d1n0.ui.boilerplate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

inline fun <T> MutableStateFlow<T>.updateCatching(
	errorFlow: MutableStateFlow<Throwable?>,
	block: (T) -> T,
) = runCatching { update(block) }
	.onSuccess { errorFlow.update { null } }
	.onFailure { e -> errorFlow.update { e } }