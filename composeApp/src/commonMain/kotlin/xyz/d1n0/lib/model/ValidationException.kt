package xyz.d1n0.lib.model

import kotlin.time.Duration

/**
 * A value the watch can't store, thrown by the models when they are created so the UI can say
 * what is wrong in the user's language. The message stays English, for logs.
 *
 * They are [IllegalArgumentException]s, like the `require` checks they replace.
 */
sealed class ValidationException(message: String) : IllegalArgumentException(message)

/** Text longer than [maxLength] characters, e.g. a reminder title. */
class TextTooLongException(val maxLength: Int, message: String) : ValidationException(message)

/** A duration of [limit] or more, the limit itself isn't allowed either. */
class DurationTooLongException(val limit: Duration, message: String) : ValidationException(message)

/** A number outside [range]. */
class ValueOutOfRangeException(val range: IntRange, message: String) : ValidationException(message)
