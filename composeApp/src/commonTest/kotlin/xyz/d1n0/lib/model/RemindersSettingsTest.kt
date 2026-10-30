package xyz.d1n0.lib.model

import kotlinx.datetime.LocalDate
import xyz.d1n0.lib.constant.ReminderDayOfWeek
import xyz.d1n0.lib.constant.ReminderRecurrence
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class RemindersSettingsTest {
	private val titlePacket = "30014D454554494E470000000000000000000000".hexToByteArray()
	private val configPacket = "3101052503252503252200".hexToByteArray()

	private val config = ReminderConfig(
		enable = true,
		recurrence = ReminderRecurrence.REPEAT_WEEKLY,
		startDate = LocalDate(2025, 3, 25),
		endDate = LocalDate(2025, 3, 25),
		daysOfWeek = setOf(ReminderDayOfWeek.MONDAY, ReminderDayOfWeek.FRIDAY),
	)

	@Test
	fun titleRoundTripsWithPadding() {
		val title = ReminderTitle("MEETING")

		assertEquals(18, title.bytes.size)
		assertEquals(title, ReminderTitle.fromBytes(title.bytes))
		assertContentEquals(titlePacket.sliceArray(2..titlePacket.lastIndex), title.bytes)
	}

	@Test
	fun titleValidatesLengths() {
		assertFailsWith<IllegalArgumentException> { ReminderTitle("1234567890123456789") }
		assertFailsWith<IllegalArgumentException> { ReminderTitle.fromBytes(ByteArray(17)) }
	}

	@Test
	fun configRoundTripsDatesRecurrenceAndDays() {
		assertContentEquals(configPacket.sliceArray(2..configPacket.lastIndex), config.bytes)

		val parsed = ReminderConfig.fromBytes(config.bytes)
		assertEquals(config, parsed)
		assertEquals("Mon, Fri", parsed.daysOfWeekDisplayString)
	}

	@Test
	fun emptyDaysDisplayNever() {
		assertEquals("Never", config.copy(daysOfWeek = emptySet()).daysOfWeekDisplayString)
	}

	@Test
	fun endDateCannotPrecedeStartDate() {
		val adjusted = config.copy(endDate = LocalDate(2025, 3, 1))

		assertEquals(adjusted.startDate, adjusted.endDate)
	}

	@Test
	fun unsetDatesFallBackToWatchDefault() {
		val parsed = ReminderConfig.fromBytes("000000000000000000".hexToByteArray())

		assertEquals(ReminderConfig.DEFAULT_DATE, parsed.startDate)
		assertEquals(ReminderConfig.DEFAULT_DATE, parsed.endDate)
	}

	@Test
	fun configRejectsInvalidLength() {
		assertFailsWith<IllegalArgumentException> { ReminderConfig.fromBytes(ByteArray(8)) }
	}

	@Test
	fun parsesReminderPacketsByOneBasedPosition() {
		val settings = RemindersSettings.empty(2)
			.parseReminderTitlePacket(titlePacket)
			.parseReminderConfigPacket(configPacket)

		assertEquals(ReminderTitle("MEETING"), settings.reminderTitles[0])
		assertEquals(config, settings.reminderConfigs[0])
		assertEquals(null, settings.reminderTitles[1])
		assertEquals(null, settings.reminderConfigs[1])
	}

	@Test
	fun initializedRemindersWritePackets() {
		val settings = RemindersSettings(
			reminderTitles = listOf(ReminderTitle("MEETING"), ReminderTitle("DINNER")),
			reminderConfigs = listOf(config, config.copy(enable = false)),
		)

		assertContentEquals(titlePacket, settings.reminderTitlePackets[0])
		assertContentEquals(configPacket, settings.reminderConfigPackets[0])
		assertEquals(2, settings.reminderTitlePackets[1][1].toInt())
		assertEquals(2, settings.reminderConfigPackets[1][1].toInt())
	}

	@Test
	fun reminderCombinesTitleAndConfig() {
		val reminder = Reminder(ReminderTitle("MEETING"), config)

		assertEquals("MEETING", reminder.title.value)
		assertEquals(config, reminder.config)
	}

	@Test
	fun rejectsMalformedReminderPackets() {
		assertFailsWith<IllegalArgumentException> {
			RemindersSettings.empty(2).parseReminderTitlePacket(titlePacket.copyOf().also { it[0] = 0 })
		}
		assertFailsWith<IllegalArgumentException> {
			RemindersSettings.empty(2).parseReminderTitlePacket(titlePacket.copyOf(19))
		}
		assertFailsWith<IllegalArgumentException> {
			RemindersSettings.empty(2).parseReminderTitlePacket(titlePacket.copyOf().also { it[1] = 3 })
		}
		assertFailsWith<IllegalArgumentException> {
			RemindersSettings.empty(2).parseReminderConfigPacket(configPacket.copyOf().also { it[0] = 0 })
		}
		assertFailsWith<IllegalArgumentException> {
			RemindersSettings.empty(2).parseReminderConfigPacket(configPacket.copyOf(10))
		}
		assertFailsWith<IllegalArgumentException> {
			RemindersSettings.empty(2).parseReminderConfigPacket(configPacket.copyOf().also { it[1] = 0 })
		}
	}

	@Test
	fun uninitializedRemindersCannotBuildPackets() {
		assertFailsWith<IllegalArgumentException> { RemindersSettings.empty(1).reminderTitlePackets }
		assertFailsWith<IllegalArgumentException> { RemindersSettings.empty(1).reminderConfigPackets }
	}
}
