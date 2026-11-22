package xyz.d1n0.phone

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import xyz.d1n0.data.database.AppSettingsDao
import xyz.d1n0.data.database.FakeAppSettingsDao
import xyz.d1n0.data.repository.AppSettingsRepository
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FindPhoneSoundsTest {
	private val builtIn = byteArrayOf(0)
	private val song = byteArrayOf(1, 2, 3)
	private val store = FakeCustomSoundStore()
	private val dao = FakeAppSettingsDao()

	/** Runs before the next sound name write, once, e.g. to throw or to suspend. */
	private var beforeNameWrite: (suspend () -> Unit)? = null
	private val settings = AppSettingsRepository(object : AppSettingsDao by dao {
		override suspend fun setFindPhoneSoundName(name: String?) {
			beforeNameWrite?.let { beforeNameWrite = null; it() }
			dao.setFindPhoneSoundName(name)
		}
	})
	private val ringer = FakePhoneRinger()
	private val sounds = FindPhoneSounds(store, settings, ringer, loadBuiltIn = { builtIn })

	private var readLimit: Int? = null

	private fun picked(name: String = "Song.MP3", bytes: ByteArray = song, size: Long? = bytes.size.toLong()) =
		PickedAudio(displayName = name, sizeBytes = size, read = { maxBytes ->
			readLimit = maxBytes
			bytes.copyOf(minOf(maxBytes, bytes.size))
		})

	@Test
	fun aPlayableFileIsCopiedAndNamed() = runTest {
		assertNull(sounds.choose(picked()))

		assertContentEquals(song, store.stored)
		assertEquals("mp3", store.extension)
		assertEquals("Song.MP3", settings.findPhoneSettings().soundName)
		assertContentEquals(song, sounds.customSound())
	}

	@Test
	fun aFileTooLargeIsRejectedBeforeReadingIt() = runTest {
		var read = false
		val huge = PickedAudio("Huge.wav", FindPhoneSounds.MAX_SIZE_BYTES + 1L) { read = true; song }

		assertEquals(FindPhoneSounds.Rejection.TooLarge, sounds.choose(huge))
		assertFalse(read)
	}

	@Test
	fun aFileTooLargeWithoutAKnownSizeIsRejectedAfterReadingPastTheLimit() = runTest {
		val bytes = ByteArray(FindPhoneSounds.MAX_SIZE_BYTES + 100)

		assertEquals(FindPhoneSounds.Rejection.TooLarge, sounds.choose(picked(bytes = bytes, size = null)))
		assertEquals(FindPhoneSounds.MAX_SIZE_BYTES + 1, readLimit)
		assertNull(store.stored)
	}

	@Test
	fun aFileExactlyAtTheLimitIsTaken() = runTest {
		val bytes = ByteArray(FindPhoneSounds.MAX_SIZE_BYTES)

		assertNull(sounds.choose(picked(bytes = bytes, size = null)))
		assertEquals(FindPhoneSounds.MAX_SIZE_BYTES, store.stored?.size)
	}

	@Test
	fun aFailedNameWriteKeepsThePreviousSound() = runTest {
		sounds.choose(picked())
		beforeNameWrite = { error("database locked") }

		assertIs<FindPhoneSounds.Rejection.Failed>(sounds.choose(picked(name = "Other.wav", bytes = byteArrayOf(9))))

		assertContentEquals(song, store.stored)
		assertEquals("mp3", store.extension)
		assertEquals("Song.MP3", settings.findPhoneSettings().soundName)
	}

	@Test
	fun aFailedNameWriteForTheFirstSoundKeepsTheBuiltInTone() = runTest {
		beforeNameWrite = { error("database locked") }

		assertIs<FindPhoneSounds.Rejection.Failed>(sounds.choose(picked()))

		assertNull(store.stored)
		assertNull(settings.findPhoneSettings().soundName)
	}

	@OptIn(ExperimentalCoroutinesApi::class) // runCurrent
	@Test
	fun cancellingAfterTheSaveKeepsThePreviousSound() = runTest {
		sounds.choose(picked())
		beforeNameWrite = { awaitCancellation() }

		val choosing = launch { sounds.choose(picked(name = "Other.wav", bytes = byteArrayOf(9))) }
		runCurrent()
		// saved, waiting on the name
		assertContentEquals(byteArrayOf(9), store.stored)
		choosing.cancel()
		runCurrent()

		assertContentEquals(song, store.stored)
		assertEquals("mp3", store.extension)
		assertEquals("Song.MP3", settings.findPhoneSettings().soundName)
	}

	@Test
	fun rejectedFilesKeepThePreviousSound() = runTest {
		sounds.choose(picked())

		ringer.playable = { false }
		assertEquals(FindPhoneSounds.Rejection.Unplayable, sounds.choose(picked(name = "Other.ogg", bytes = byteArrayOf(9))))
		ringer.playable = { true }
		val unreadable = PickedAudio("Gone.mp3", 10) { error("deleted meanwhile") }
		assertIs<FindPhoneSounds.Rejection.Failed>(sounds.choose(unreadable))
		store.failure = IllegalStateException("disk full")
		assertIs<FindPhoneSounds.Rejection.Failed>(sounds.choose(picked(name = "Third.mp3", bytes = byteArrayOf(7))))

		assertContentEquals(song, store.stored)
		assertEquals("Song.MP3", settings.findPhoneSettings().soundName)
	}

	@Test
	fun withoutANameTheBuiltInToneRings() = runTest {
		// a copy left behind doesn't count without its name
		store.stored = song

		assertNull(sounds.customSound())
		assertFalse(sounds.isCustomSoundMissing())
	}

	@Test
	fun aMissingCopyIsReportedAndRingsTheBuiltInTone() = runTest {
		sounds.choose(picked())
		store.stored = null

		assertTrue(sounds.isCustomSoundMissing())
		assertNull(sounds.customSound())
	}

	@Test
	fun resetDeletesTheCopyAndTheName() = runTest {
		sounds.choose(picked())

		sounds.reset()

		assertNull(store.stored)
		assertNull(settings.findPhoneSettings().soundName)
		assertFalse(sounds.isCustomSoundMissing())
	}

	@OptIn(ExperimentalCoroutinesApi::class) // runCurrent
	@Test
	fun playAtCurrentVolumePlaysTheCurrentSound() = runTest {
		backgroundScope.launch { sounds.playAtCurrentVolume() }
		runCurrent()
		assertContentEquals(builtIn, ringer.sounds.last())
		assertEquals(false, ringer.maxVolumes.last())
		assertEquals(false, ringer.vibrates.last())

		sounds.choose(picked())
		backgroundScope.launch { sounds.playAtCurrentVolume() }
		runCurrent()
		assertContentEquals(song, ringer.sounds.last())
	}

	@Test
	fun extensionIsAPlayerHintOnly() {
		assertEquals("mp3", FindPhoneSounds.extensionOf("Song.MP3"))
		assertEquals("m4a", FindPhoneSounds.extensionOf("voice memo.m4a"))
		assertEquals("", FindPhoneSounds.extensionOf("no extension"))
		assertEquals("", FindPhoneSounds.extensionOf("odd.名字"))
		assertEquals("", FindPhoneSounds.extensionOf("archive.tar.toolong"))
	}
}
