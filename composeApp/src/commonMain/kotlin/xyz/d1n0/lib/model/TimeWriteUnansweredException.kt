package xyz.d1n0.lib.model

import com.juul.kable.NotConnectedException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * The time packet was written, and the watch disconnected without answering it.
 *
 * An older GMW-B5000 module (3459) does this after a lower-right short press: it takes the time,
 * then its radio goes silent until the link times out, which Android reports as a failed write
 * (`GATT_ERROR(133)`). The official app sees the same in a capture of that watch.
 */
class TimeWriteUnansweredException(cause: Throwable) :
	IllegalStateException("The watch disconnected without answering the time write", cause)

/** How long after a failed time write the watch has to be disconnected for it to count as unanswered. */
private val UNANSWERED_DISCONNECT_WAIT = 1.seconds

/**
 * What a failed time write throws: [TimeWriteUnansweredException] if the watch is disconnected
 * within a second, [error] otherwise. Android fails the pending write and reports the disconnect
 * at the same moment, so the disconnect may only arrive just after the failure.
 *
 * A [NotConnectedException] is always [error]: Kable throws it when the link was already gone, so
 * the time packet was never sent.
 *
 * @param awaitDisconnected Returns once the watch is disconnected.
 */
internal suspend fun timeWriteFailure(error: Exception, awaitDisconnected: suspend () -> Unit): Exception =
	if (error !is NotConnectedException &&
		withTimeoutOrNull(UNANSWERED_DISCONNECT_WAIT) { awaitDisconnected() } != null
	) {
		TimeWriteUnansweredException(error)
	} else {
		error
	}
