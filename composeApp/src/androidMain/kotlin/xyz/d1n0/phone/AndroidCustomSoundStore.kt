package xyz.d1n0.phone

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Keeps the copy in filesDir/find_phone/custom.<ext>, excluded from backups by
 * res/xml/data_extraction_rules.xml: Auto Backup gives up on the whole app once it's over its
 * 25 MB quota, the database with the installation id included.
 */
class AndroidCustomSoundStore(context: Context) : CustomSoundStore {
	private val directory = File(context.applicationContext.filesDir, DIRECTORY)

	override suspend fun save(bytes: ByteArray, extension: String) = withContext(Dispatchers.IO) {
		directory.mkdirs()
		val temporary = File(directory, TEMPORARY)
		temporary.writeBytes(bytes)
		val target = File(directory, if (extension.isEmpty()) NAME else "$NAME.$extension")
		// replaces a copy of the same name at once, the old one stays if this fails
		check(temporary.renameTo(target)) { "Couldn't store the sound" }
		// only one copy, the one just written, an older one may have another extension
		copies().filter { it != target }.forEach { it.delete() }
	}

	override suspend fun load(): ByteArray? = withContext(Dispatchers.IO) {
		copies().firstOrNull()?.let { runCatching { it.readBytes() }.getOrNull() }
	}

	override suspend fun exists(): Boolean = withContext(Dispatchers.IO) { copies().isNotEmpty() }

	override suspend fun delete() {
		withContext(Dispatchers.IO) { directory.listFiles()?.forEach { it.delete() } }
	}

	private fun copies(): List<File> =
		directory.listFiles()?.filter { it.name == NAME || it.name.startsWith("$NAME.") }.orEmpty()

	private companion object {
		/** Also in res/xml/data_extraction_rules.xml. */
		const val DIRECTORY = "find_phone"
		const val NAME = "custom"
		const val TEMPORARY = "saving.tmp"
	}
}
