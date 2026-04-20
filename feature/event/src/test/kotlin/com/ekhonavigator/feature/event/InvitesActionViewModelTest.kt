package com.ekhonavigator.feature.event

import app.cash.turbine.test
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.testing.MainDispatcherRule
import com.ekhonavigator.core.testing.TestCalendarRepository
import com.ekhonavigator.core.testing.testCalendarEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [InvitesActionViewModel].
 *
 * The action VM drives the badge/dot on the invites entry point — it must
 * only count events that still need a user decision (RSVP = PENDING).
 * Over-counting shows a badge that never clears; under-counting hides
 * invites the user never saw.
 */
class InvitesActionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `pendingCount reflects number of PENDING invites`() = runTest {
        val calendarRepository = TestCalendarRepository()
        val viewModel = InvitesActionViewModel(calendarRepository)

        viewModel.pendingCount.test {
            assertEquals(0, awaitItem())

            calendarRepository.emit(
                listOf(
                    testCalendarEvent(id = "1", myRsvpStatus = RsvpStatus.PENDING),
                    testCalendarEvent(id = "2", myRsvpStatus = RsvpStatus.PENDING),
                    testCalendarEvent(id = "3", myRsvpStatus = RsvpStatus.GOING),
                    testCalendarEvent(id = "4", myRsvpStatus = RsvpStatus.NOT_GOING),
                ),
            )

            assertEquals(2, awaitItem())
        }
    }

    @Test
    fun `pendingCount stays at zero when every invite has a decision`() = runTest {
        val calendarRepository = TestCalendarRepository()
        val viewModel = InvitesActionViewModel(calendarRepository)

        // First produce a non-zero count so the flow is actively collecting,
        // then drop back to zero-pending. Going 0 → N → 0 exercises the
        // filter in both directions; StateFlow would dedupe 0 → 0 directly.
        viewModel.pendingCount.test {
            assertEquals(0, awaitItem())

            calendarRepository.emit(
                listOf(testCalendarEvent(id = "1", myRsvpStatus = RsvpStatus.PENDING)),
            )
            assertEquals(1, awaitItem())

            calendarRepository.emit(
                listOf(
                    testCalendarEvent(id = "1", myRsvpStatus = RsvpStatus.GOING),
                    testCalendarEvent(id = "2", myRsvpStatus = RsvpStatus.NOT_GOING),
                ),
            )
            assertEquals(0, awaitItem())
        }
    }
}
