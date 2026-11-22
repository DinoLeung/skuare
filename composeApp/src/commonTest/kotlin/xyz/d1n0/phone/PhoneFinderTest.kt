package xyz.d1n0.phone

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class) // advanceTimeBy, runCurrent, currentTime
class PhoneFinderTest {
	private val tone = byteArrayOf(1, 2, 3)
	private val ringer = FakePhoneRinger()

	/** On the test's virtual time, in its background scope, so a ring left running doesn't hang the test. */
	private fun TestScope.phoneFinder(customSound: suspend () -> ByteArray? = { null }) = PhoneFinder(
		ringer,
		loadSound = { tone },
		scope = backgroundScope,
		loadCustomSound = customSound,
		clock = object : Clock {
			override fun now() = Instant.fromEpochMilliseconds(currentTime)
		},
	)

	@Test
	fun ringsForTheDurationThenStops() = runTest {
		val finder = phoneFinder()

		finder.ring(30.seconds)
		runCurrent()
		assertTrue(finder.ringing.value)
		assertContentEquals(tone, ringer.sounds.single())

		advanceTimeBy(29.seconds)
		assertTrue(finder.ringing.value)
		advanceTimeBy(2.seconds)
		assertFalse(finder.ringing.value)
		assertEquals(1, ringer.cancelCount)
	}

	@Test
	fun stopEndsItEarly() = runTest {
		val finder = phoneFinder()
		finder.ring(30.seconds)
		runCurrent()

		finder.stop()

		assertFalse(finder.ringing.value)
		assertEquals(1, ringer.cancelCount)
		// the old timer doesn't fire later
		advanceTimeBy(60.seconds)
		assertEquals(1, ringer.cancelCount)
	}

	@Test
	fun ringingAgainRestartsTheTimerWithoutRestartingTheSound() = runTest {
		val finder = phoneFinder()
		finder.ring(30.seconds)
		runCurrent()

		advanceTimeBy(20.seconds)
		finder.ring(30.seconds)
		// past the first ring's end, the second one's timer counts from its own press
		advanceTimeBy(20.seconds)
		assertTrue(finder.ringing.value)
		assertEquals(1, ringer.sounds.size)
		assertEquals(0, ringer.cancelCount)

		advanceTimeBy(11.seconds)
		assertFalse(finder.ringing.value)
		assertEquals(1, ringer.cancelCount)
	}

	@Test
	fun ringingAgainTakesTheNewLength() = runTest {
		val finder = phoneFinder()
		finder.ring(60.seconds)
		runCurrent()

		finder.ring(10.seconds)
		advanceTimeBy(11.seconds)

		assertFalse(finder.ringing.value)
	}

	@Test
	fun untilStoppedRingsUntilStop() = runTest {
		val finder = phoneFinder()
		finder.ring(null)
		runCurrent()

		advanceTimeBy(1.days)
		assertTrue(finder.ringing.value)

		finder.stop()
		assertFalse(finder.ringing.value)
	}

	@Test
	fun aTimedRingAfterUntilStoppedGetsATimer() = runTest {
		val finder = phoneFinder()
		finder.ring(null)
		runCurrent()

		finder.ring(10.seconds)
		advanceTimeBy(11.seconds)

		assertFalse(finder.ringing.value)
	}

	@Test
	fun ringingAfterAStopStartsTheSoundAgain() = runTest {
		val finder = phoneFinder()
		finder.ring(30.seconds)
		runCurrent()
		finder.stop()

		finder.ring(30.seconds)
		runCurrent()

		assertTrue(finder.ringing.value)
		assertEquals(2, ringer.sounds.size)
	}

	@Test
	fun stopWhenNotRingingDoesNothing() = runTest {
		val finder = phoneFinder()

		finder.stop()

		assertFalse(finder.ringing.value)
		assertTrue(ringer.sounds.isEmpty())
	}

	@Test
	fun aFailingRingerEndsTheRinging() = runTest {
		ringer.failure = IllegalStateException("no audio output")
		val finder = phoneFinder()

		finder.ring(30.seconds)
		runCurrent()

		assertFalse(finder.ringing.value)
		// a later ring tries again instead of being stuck behind the failed one
		ringer.failure = null
		finder.ring(30.seconds)
		runCurrent()
		assertTrue(finder.ringing.value)
	}

	@Test
	fun ringEndsAtFollowsTheTimer() = runTest {
		val finder = phoneFinder()
		assertNull(finder.ringEndsAt.value)

		finder.ring(30.seconds)
		assertEquals(Instant.fromEpochMilliseconds(30_000), finder.ringEndsAt.value)

		// a second ring moves it to its own end
		advanceTimeBy(20.seconds)
		finder.ring(30.seconds)
		assertEquals(Instant.fromEpochMilliseconds(50_000), finder.ringEndsAt.value)

		// until stopped has no end
		finder.ring(null)
		assertNull(finder.ringEndsAt.value)

		finder.ring(10.seconds)
		advanceTimeBy(11.seconds)
		assertNull(finder.ringEndsAt.value)
		assertFalse(finder.ringing.value)
	}

	@Test
	fun ringsWithTheCustomSoundWhenThereIsOne() = runTest {
		val song = byteArrayOf(7, 8)
		val finder = phoneFinder(customSound = { song })

		finder.ring(30.seconds)
		runCurrent()

		assertContentEquals(song, ringer.sounds.single())
		assertTrue(finder.ringing.value)
	}

	@Test
	fun aCustomSoundThatFailsToPlayFallsBackToTheBuiltInTone() = runTest {
		val song = byteArrayOf(7, 8)
		ringer.failure = IllegalStateException("unsupported format")
		ringer.failsOn = song
		val finder = phoneFinder(customSound = { song })

		finder.ring(30.seconds)
		runCurrent()

		// it never stays silent
		assertEquals(2, ringer.sounds.size)
		assertContentEquals(tone, ringer.sounds.last())
		assertTrue(finder.ringing.value)
	}

	@Test
	fun aCustomSoundThatFailsToLoadFallsBackToTheBuiltInTone() = runTest {
		val finder = phoneFinder(customSound = { error("copy unreadable") })

		finder.ring(30.seconds)
		runCurrent()

		assertContentEquals(tone, ringer.sounds.single())
		assertTrue(finder.ringing.value)
	}

	@Test
	fun ringsAtTheVolumeAskedForAndAlwaysVibrates() = runTest {
		val finder = phoneFinder()

		finder.ring(30.seconds, forceMaxVolume = false)
		runCurrent()

		assertEquals(listOf(false), ringer.maxVolumes)
		assertEquals(listOf(true), ringer.vibrates)
		// restarting the timer keeps the volume the ringing started with
		finder.ring(30.seconds)
		runCurrent()
		assertEquals(listOf(false), ringer.maxVolumes)
	}
}
