package xyz.d1n0.lib.helper

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionAndDateHelpersTest {
	@Test
	fun replaceAtReturnsAListWithOnlyTheRequestedElementChanged() {
		val original = listOf("first", "second", "third")

		val replaced = original.replaceAt(1, "updated")

		assertEquals(listOf("first", "updated", "third"), replaced)
		assertEquals(listOf("first", "second", "third"), original)
		assertFalse(replaced === original)
	}

	@Test
	fun replaceAtLeavesListUnchangedForMissingIndex() {
		assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).replaceAt(-1, 9))
		assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).replaceAt(3, 9))
	}

	@Test
	fun dateMillisUsesUtcEpochDays() {
		assertEquals(0L, LocalDate(1970, 1, 1).toMillis())
		assertEquals(86_400_000L, LocalDate(1970, 1, 2).toMillis())
		assertEquals(-86_400_000L, LocalDate(1969, 12, 31).toMillis())
	}

	@Test
	fun dateMillisRoundTripsBeforeAndAfterEpoch() {
		listOf(
			LocalDate(1900, 1, 1),
			LocalDate(1969, 12, 31),
			LocalDate(1970, 1, 1),
			LocalDate(2024, 2, 29),
		).forEach { date ->
			assertEquals(date, LocalDate.fromMillis(date.toMillis()))
		}
	}

	@Test
	fun fromMillisUsesTheContainingUtcDay() {
		assertEquals(LocalDate(1970, 1, 1), LocalDate.fromMillis(86_399_999L))
		assertEquals(LocalDate(1969, 12, 31), LocalDate.fromMillis(-1L))
	}

	@Test
	fun requireInReturnsTheReceiverInsideInclusiveRange() {
		var messageEvaluated = false
		val value = 5.requireIn(1..5) {
			messageEvaluated = true
			"not used"
		}

		assertEquals(5, value)
		assertFalse(messageEvaluated)
	}

	@Test
	fun requireInThrowsWithLazyMessageOutsideRange() {
		val marker = Any()
		val error = assertFailsWith<IllegalArgumentException> {
			6.requireIn(1..5) { marker }
		}

		assertEquals(marker.toString(), error.message)
		assertTrue(error.message?.isNotEmpty() == true)
	}
}
