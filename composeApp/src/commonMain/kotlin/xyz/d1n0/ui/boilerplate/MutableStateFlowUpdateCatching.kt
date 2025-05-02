package xyz.d1n0.ui.boilerplate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

inline fun <S, R> MutableStateFlow<S>.updateCatching(
	crossinline transform: (S) -> R,
	crossinline onSuccess: S.(R) -> S,
	crossinline onFailure: S.(Throwable) -> S,
) {
	this.update { state ->
		runCatching { transform(state) }
			.fold(
				onSuccess = { value -> state.onSuccess(value) },
				onFailure = { err -> state.onFailure(err) }
			)
	}
}