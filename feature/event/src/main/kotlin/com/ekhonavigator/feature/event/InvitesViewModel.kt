package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.model.CanvasAnnouncement
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.canvas.CanvasAnnouncementRepository
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.RsvpStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvitesViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository,
    private val canvasCourseRepository: CanvasCourseRepository,
    private val canvasAnnouncementRepository: CanvasAnnouncementRepository,
) : ViewModel() {

    init {
        // Same per-course announcement fan-out as CoursesHomeViewModel —
        // whichever surface the user opens first kicks it off.
        viewModelScope.launch {
            val seen = mutableSetOf<String>()
            canvasCourseRepository.observeCourses().collect { courses ->
                val newOnes = courses.filter { it.id !in seen }
                if (newOnes.isEmpty()) return@collect
                seen += newOnes.map { it.id }
                newOnes.forEach { course ->
                    launch { runCatching { canvasAnnouncementRepository.sync(course.id) } }
                }
            }
        }
    }

    private val _showPast = MutableStateFlow(false)
    val showPast: StateFlow<Boolean> = _showPast.asStateFlow()

    val pendingInvites: StateFlow<List<CalendarEvent>> = _showPast
        .flatMapLatest { calendarRepository.observePendingInvites(includePast = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val declinedInvites: StateFlow<List<CalendarEvent>> = _showPast
        .flatMapLatest { calendarRepository.observeDeclinedInvites(includePast = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val friendRequests: StateFlow<List<FriendRequest>> = run {
        val uid = authRepository.getCurrentUserUid()
        if (uid == null) {
            MutableStateFlow(emptyList())
        } else {
            socialRepository.observeIncomingRequests(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }
    }

    val announcements: StateFlow<List<CanvasAnnouncement>> =
        canvasAnnouncementRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Lookup so each row can show its course label without a second VM.
    val courseCodeById: StateFlow<Map<String, String>> =
        canvasCourseRepository.observeCourses()
            .map { it.associate { course -> course.id to course.code } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun markAnnouncementRead(announcementId: String) {
        viewModelScope.launch {
            canvasAnnouncementRepository.markRead(announcementId)
        }
    }

    fun togglePast() {
        _showPast.value = !_showPast.value
    }

    fun rsvp(eventId: String, status: RsvpStatus) {
        val uid = authRepository.getCurrentUserUid() ?: return
        val displayName = authRepository.getCurrentUserDisplayName() ?: ""
        viewModelScope.launch {
            customEventRepository.rsvp(eventId, uid, displayName, status)
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            socialRepository.acceptFriendRequest(uid, fromUserId)
        }
    }

    fun denyFriendRequest(fromUserId: String) {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            socialRepository.denyFriendRequest(uid, fromUserId)
        }
    }
}
