package xyz.d1n0.model

import xyz.d1n0.lib.model.AppInfo
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class AppInfoTest {
	// captured from the official app, same value on all 4 watches paired from one phone
	private val officialApp = "22f33eb0ab2254d9dc03c402".hexToByteArray()
	private val resetWatch = "22ffffffffffffffffffff00".hexToByteArray()

	@Test
	fun parsesIdAndFlag() {
		val appInfo = AppInfo.fromPacket(officialApp)
		assertContentEquals("f33eb0ab2254d9dc03c4".hexToByteArray(), appInfo.id)
		assertEquals(AppInfo.PAIRED_FLAG, appInfo.flag)
	}

	@Test
	fun detectsResetWatch() {
		assertTrue(AppInfo.fromPacket(resetWatch).isReset)
		assertFalse(AppInfo.fromPacket(officialApp).isReset)
	}

	@Test
	fun buildsPacket() {
		val appInfo = AppInfo(id = "f33eb0ab2254d9dc03c4".hexToByteArray())
		assertContentEquals(officialApp, appInfo.packet)
	}

	@Test
	fun comparesByContent() {
		assertEquals(AppInfo.fromPacket(officialApp), AppInfo.fromPacket(officialApp.copyOf()))
		assertNotEquals(AppInfo.fromPacket(officialApp), AppInfo.fromPacket(resetWatch))
	}

	@Test
	fun idCannotBeChangedFromOutside() {
		val id = "f33eb0ab2254d9dc03c4".hexToByteArray()
		val appInfo = AppInfo(id = id)
		id[0] = 0x00
		appInfo.id[1] = 0x00
		assertContentEquals(officialApp, appInfo.packet)
	}

	@Test
	fun rejectsInvalidPackets() {
		assertFailsWith<IllegalArgumentException> { AppInfo.fromPacket(officialApp.copyOf(11)) }
		assertFailsWith<IllegalArgumentException> {
			AppInfo.fromPacket(officialApp.copyOf().also { it[0] = 0x10 })
		}
		assertFailsWith<IllegalArgumentException> { AppInfo(id = ByteArray(9)) }
	}
}
