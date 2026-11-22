package xyz.d1n0.phone

/** In-memory [CustomSoundStore], [failure] makes [save] throw, keeping the stored copy. */
internal class FakeCustomSoundStore : CustomSoundStore {
	var stored: ByteArray? = null
	var extension: String? = null
	var failure: Exception? = null

	override suspend fun save(bytes: ByteArray, extension: String) {
		failure?.let { throw it }
		stored = bytes
		this.extension = extension
	}

	override suspend fun load(): ByteArray? = stored

	override suspend fun exists() = stored != null

	override suspend fun delete() {
		stored = null
		extension = null
	}
}
