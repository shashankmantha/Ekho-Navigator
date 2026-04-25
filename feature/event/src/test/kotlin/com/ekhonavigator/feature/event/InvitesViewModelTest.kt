package com.ekhonavigator.feature.event

import app.cash.turbine.test
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.testing.MainDispatcherRule
import com.ekhonavigator.core.testing.TestAuthRepository
import com.ekhonavigator.core.testing.TestCalendarRepository
import com.ekhonavigator.core.testing.TestCustomEventRepository
import com.ekhonavigator.core.testing.TestSocialRepository
import com.ekhonavigator.core.testing.testCalendarEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [InvitesViewModel].
 *
 * Covers the invites tab: pending and declined lists must filter by RSVP
 * status, and `rsvp()` must delegate through to [TestCustomEventRepository]
 * with the right user identity.
 */
class InvitesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var calendarRepository: TestCalendarRepository
    private lateinit var customEventRepository: TestCustomEventRepository
    private lateinit var socialRepository: TestSocialRepository
    private lateinit var authRepository: TestAuthRepository
    private lateinit var viewModel: InvitesViewModel

    @Before
    fun setup() {
        calendarRepository = TestCalendarRepository()
        customEventRepository = TestCustomEventRepository()
        socialRepository = TestSocialRepository()
        authRepository = TestAuthRepository()
        viewModel = InvitesViewModel(
            calendarRepository,
            customEventRepository,
            socialRepository,
            authRepository,
        )
    }

    @Test
    fun `pendingInvites filters to events with PENDING RSVP`() = runTest {
        val pending = testCalendarEvent(id = "pending-1", myRsvpStatus = RsvpStatus.PENDING)
        val going = testCalendarEvent(id = "going-1", myRsvpStatus = RsvpStatus.GOING)
        val declined = testCalendarEvent(id = "declined-1", myRsvpStatus = RsvpStatus.NOT_GOING)

        viewModel.pendingInvites.test {
            assertEquals(emptyList<Any>(), awaitItem()) // initial
            calendarRepository.emit(listOf(pending, going, declined))
            assertEquals(listOf(pending), awaitItem())
        }
    }

    @Test
    fun `declinedInvites filters to events with NOT_GOING RSVP`() = runTest {
        val declined = testCalendarEvent(id = "declined-1", myRsvpStatus = RsvpStatus.NOT_GOING)
        val other = testCalendarEvent(id = "other-1", myRsvpStatus = RsvpStatus.PENDING)

        viewModel.declinedInvites.test {
            assertEquals(emptyList<Any>(), awaitItem())
            calendarRepository.emit(listOf(declined, other))
            assertEquals(listOf(declined), awaitItem())
        }
    }

    @Test
    fun `rsvp delegates to custom event repository with current user identity`() = runTest {
        authRepository.uid = "user-42"
        authRepository.displayName = "Alice"

        viewModel.rsvp(eventId = "evt-99", status = RsvpStatus.GOING)

        assertEquals(1, customEventRepository.rsvpCalls.size)
        val call = customEventRepository.rsvpCalls.first()
        assertEquals("evt-99", call.eventId)
        assertEquals("user-42", call.userId)
        assertEquals("Alice", call.displayName)
        assertEquals(RsvpStatus.GOING, call.status)
    }

    @Test
    fun `rsvp is a no-op when the user is signed out`() = runTest {
        // Safety guard: calling rsvp with no signed-in user must not write
        // an invalid attendee record (empty uid would corrupt Firestore).
        authRepository.uid = null

        viewModel.rsvp(eventId = "evt-99", status = RsvpStatus.GOING)

        assertTrue(customEventRepository.rsvpCalls.isEmpty())
    }

    @Test
    fun `togglePast surfaces past pending invites that are hidden by default`() = runTest {
        val past = testCalendarEvent(
            id = "old",
            myRsvpStatus = RsvpStatus.PENDING,
            startTime = Instant.parse("2020-01-01T00:00:00Z"),
            endTime = Instant.parse("2020-01-01T01:00:00Z"),
        )
        val upcoming = testCalendarEvent(id = "new", myRsvpStatus = RsvpStatus.PENDING)

        viewModel.pendingInvites.test {
            assertEquals(emptyList<Any>(), awaitItem())
            calendarRepository.emit(listOf(past, upcoming))
            assertEquals(listOf(upcoming), awaitItem())

            viewModel.togglePast()
            assertEquals(listOf(past, upcoming), awaitItem())
        }
    }

    @Test
    fun `rsvp falls back to empty displayName when auth has none`() = runTest {
        authRepository.uid = "user-42"
        authRepository.displayName = null

        viewModel.rsvp(eventId = "evt-99", status = RsvpStatus.NOT_GOING)

        assertEquals("", customEventRepository.rsvpCalls.first().displayName)
    }
}
