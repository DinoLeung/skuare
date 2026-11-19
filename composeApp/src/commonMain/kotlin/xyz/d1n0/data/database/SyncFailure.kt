package xyz.d1n0.data.database

/**
 * Why the last time sync failed, see [PairedWatch.lastSyncFailure]. It is stored instead of the
 * error's message so the UI can explain it in the user's language.
 *
 * Room stores the entry's name, so renaming or removing one needs a migration.
 */
enum class SyncFailure {
	/** The watch replied with an error packet, [PairedWatch.lastSyncErrorDetail] holds its bytes. */
	WATCH_ERROR,

	/** The watch didn't reply in time. */
	TIMEOUT,

	/** The connection to the watch dropped. */
	CONNECTION_LOST,

	/** Anything else, [PairedWatch.lastSyncErrorDetail] holds the English message. */
	OTHER,
}
