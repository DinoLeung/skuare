package xyz.d1n0.lib.model

import xyz.d1n0.lib.constant.FontStyle
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WatchSettingsTest {
	// GW-BX5600 reply from the official app capture (disaply font_bx.pcapng), font Standard
	private val bxStandard = byteArrayOf(0x13, 0x17, 0, 0, 0, 0, 0, 0, 0x00, 0, 0, 0)

	// Same reply after the official app set the font to Classic
	private val bxClassic = byteArrayOf(0x13, 0x17, 0, 0, 0, 0, 0, 0, 0x20, 0, 0, 0)

	@Test
	fun readsFontStyle() {
		assertEquals(FontStyle.STANDARD, WatchSettings.fromPacket(bxStandard).fontStyle)
		assertEquals(FontStyle.CLASSIC, WatchSettings.fromPacket(bxClassic).fontStyle)
	}

	@Test
	fun writesFontStyleToByteEight() {
		val settings = WatchSettings.fromPacket(bxStandard).copy(fontStyle = FontStyle.CLASSIC)
		assertContentEquals(bxClassic, settings.packet)
	}

	@Test
	fun roundTripKeepsFontStyle() {
		assertContentEquals(bxClassic, WatchSettings.fromPacket(bxClassic).packet)
	}
}
