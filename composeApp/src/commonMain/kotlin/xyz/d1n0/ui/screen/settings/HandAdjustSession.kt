package xyz.d1n0.ui.screen.settings

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.lib.model.HandAdjuster
import xyz.d1n0.ui.component.HandAdjustUiState
import kotlin.coroutines.cancellation.CancellationException

/**
 * One hand adjust session, from picking a [group] to [finish]. Commands run on [scope], e.g. the
 * watch's, so leaving the screen doesn't cancel them halfway.
 *
 * Call [start] once, then change the steps and [set] as often as needed, then [finish].
 */
class HandAdjustSession(
	val group: HandGroup,
	private val adjuster: HandAdjuster,
	private val scope: CoroutineScope,
	private val log: Logger,
) {
	private val _state = MutableStateFlow(HandAdjustUiState(group = group))
	val state: StateFlow<HandAdjustUiState> = _state.asStateFlow()

	private var job: Job? = null
	private var isFinished = false

	/** Enters adjust mode and moves the hands to the adjusting position, see [HandAdjustUiState.isReady]. */
	fun start() {
		if (isFinished || job != null) return
		run(onSuccess = { copy(isReady = true) }) {
			adjuster.start()
			adjuster.select(group)
		}
	}

	/** One step less, see [HandAdjustUiState.canStepAnticlockwise]. */
	fun stepAnticlockwise() {
		if (isFinished || _state.value.canStepAnticlockwise == false) return
		_state.update { it.copy(steps = it.steps - 1) }
	}

	/** One step more, see [HandAdjustUiState.canStepClockwise]. */
	fun stepClockwise() {
		if (isFinished || _state.value.canStepClockwise == false) return
		_state.update { it.copy(steps = it.steps + 1) }
	}

	/**
	 * Sets the steps as typed. A value out of range is kept, so the field shows what was typed,
	 * but can't be sent, see [HandAdjustUiState.isStepsValid].
	 */
	fun setSteps(steps: Int) {
		if (isFinished || _state.value.canEditSteps == false) return
		_state.update { it.copy(steps = steps) }
	}

	/**
	 * Moves the hands by [HandAdjustUiState.steps], then confirms, see [HandAdjuster.move]. The
	 * steps go back to 0 once the hands stop, moves are relative to where they are, and stay as
	 * they were if the move fails. Ignored unless [HandAdjustUiState.canSet], and after [finish].
	 */
	fun set() {
		val current = _state.value
		if (isFinished || current.canSet == false) return
		_state.update { it.copy(isMoving = true, error = null) }
		run(onSuccess = { copy(steps = 0) }) { adjuster.move(group, current.steps) }
	}

	/**
	 * Leaves adjust mode, see [HandAdjuster.finish]. Stops waiting for the hands first if they're
	 * still moving. Only the first call does anything.
	 *
	 * @return The job sending the exit sequence, null if the session had already finished.
	 */
	fun finish(): Job? {
		if (isFinished) return null
		isFinished = true
		val pending = job
		return scope.launch {
			pending?.cancelAndJoin()
			runCatching { adjuster.finish() }
				.onFailure { log.e { "Failed to finish hand adjustment: ${it.message}" } }
		}
	}

	private fun run(
		onSuccess: HandAdjustUiState.() -> HandAdjustUiState = { this },
		block: suspend () -> Unit,
	) {
		job = scope.launch {
			val result = runCatching { block() }
			result.exceptionOrNull()?.let {
				if (it is CancellationException) throw it
				log.e { "Hand adjustment failed: ${it.message}" }
			}
			_state.update { current ->
				result.fold(
					onSuccess = { current.onSuccess().copy(isMoving = false) },
					onFailure = { current.copy(isMoving = false, error = it.message ?: "Unknown error") },
				)
			}
		}
	}
}
