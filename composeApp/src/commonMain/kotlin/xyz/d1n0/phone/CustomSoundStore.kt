package xyz.d1n0.phone

/**
 * Keeps the app's own copy of the ringtone the user picked for find phone, at most one, in
 * app-private storage. A copy, so it keeps working after the original is deleted or moved.
 * Implemented per platform, kept out of backups.
 */
interface CustomSoundStore {
	/** Replaces the stored copy. Written to a temporary file first, a failed save keeps the old one. */
	suspend fun save(bytes: ByteArray, extension: String)

	/** The stored copy, null if there is none or it can't be read. */
	suspend fun load(): ByteArray?

	suspend fun exists(): Boolean

	suspend fun delete()
}

/** For platforms without a sound picker yet, nothing is ever stored. */
class UnsupportedCustomSoundStore : CustomSoundStore {
	override suspend fun save(bytes: ByteArray, extension: String) = error("No custom sounds on this platform")
	override suspend fun load(): ByteArray? = null
	override suspend fun exists() = false
	override suspend fun delete() = Unit
}

/**
 * An audio file the user picked, not read yet, so picking never reads a large file on the UI thread.
 *
 * @property displayName The file's name, e.g. Ringtone.mp3, shown on the App settings screen.
 * @property sizeBytes Null if the platform doesn't know it before reading.
 * @property read The file's bytes, at most `maxBytes` of them, so a file larger than that is never
 *   held whole, even when [sizeBytes] is unknown or wrong. Throws if it can't be read, e.g. it was
 *   deleted meanwhile.
 */
class PickedAudio(
	val displayName: String,
	val sizeBytes: Long?,
	val read: suspend (maxBytes: Int) -> ByteArray,
)
