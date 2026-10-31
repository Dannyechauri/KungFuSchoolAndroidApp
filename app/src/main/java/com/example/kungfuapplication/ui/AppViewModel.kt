package com.example.kungfuapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kungfuapplication.data.AppSettings
import com.example.kungfuapplication.data.SchoolRepository
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.CancellationNotification
import com.example.kungfuapplication.data.model.EventNotifications
import com.example.kungfuapplication.data.model.Course
import com.example.kungfuapplication.data.model.EventPayload
import com.example.kungfuapplication.data.model.InstructorOption
import com.example.kungfuapplication.data.model.MyProfile
import com.example.kungfuapplication.data.model.Participant
import com.example.kungfuapplication.data.model.ParticipationRole
import com.example.kungfuapplication.data.model.ParticipationScope
import com.example.kungfuapplication.data.model.RecurringRule
import com.example.kungfuapplication.data.model.Room
import com.example.kungfuapplication.data.model.RulePayload
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.PerformanceConfirmer
import com.example.kungfuapplication.data.model.PerformanceDecision
import com.example.kungfuapplication.data.model.PerformanceForm
import com.example.kungfuapplication.data.model.PerformancePayload
import com.example.kungfuapplication.data.model.PerformanceRankings
import com.example.kungfuapplication.data.model.PerformanceRecord
import com.example.kungfuapplication.data.model.PerformanceRequest
import com.example.kungfuapplication.data.model.PromotionNotification
import com.example.kungfuapplication.data.model.calendarErrorText
import com.example.kungfuapplication.data.model.joinMessage
import com.example.kungfuapplication.data.model.leaveMessage
import com.example.kungfuapplication.data.remote.ApiClient
import com.example.kungfuapplication.data.remote.ApiException
import com.example.kungfuapplication.data.remote.PersistentCookieJar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

private val schoolZone: ZoneId = ZoneId.of("Europe/Madrid")

/** Las horas de la agenda llegan en hora local de Madrid, como en la web. */
fun schoolNow(): LocalDateTime = LocalDateTime.now(schoolZone)
fun schoolToday(): LocalDate = LocalDate.now(schoolZone)

fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Días que cubre la lista de próximas actividades (el backend admite hasta 93). */
const val UPCOMING_DAYS = 90L

sealed interface SessionState {
    data object Checking : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: SessionUser) : SessionState
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val lastEmail: String = "",
    val baseUrl: String = AppSettings.DEFAULT_BASE_URL,
)

data class AccountUiState(
    val isChangingPassword: Boolean = false,
    val passwordError: String? = null,
)

data class ProfileUiState(
    val profile: MyProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingFormIds: Set<Long> = emptySet(),
    val promotionNotifications: List<PromotionNotification> = emptyList(),
    val promotionNotificationError: String? = null,
    val acknowledgingPromotionIds: Set<Long> = emptySet(),
)

enum class CalendarMode { WEEK, LIST }

data class PerformanceUiState(
    val records: List<PerformanceRecord> = emptyList(),
    val rankings: PerformanceRankings? = null,
    val forms: List<PerformanceForm> = emptyList(),
    val confirmers: List<PerformanceConfirmer> = emptyList(),
    val inbox: List<PerformanceRequest> = emptyList(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val busyRecordIds: Set<Long> = emptySet(),
    val error: String? = null,
    val requestsError: String? = null,
    val formError: String? = null,
)

data class CalendarUiState(
    val selectedDate: LocalDate = schoolToday(),
    val mode: CalendarMode = CalendarMode.WEEK,
    val roomId: Long? = null,
    val rooms: List<Room> = emptyList(),
    val weekStart: LocalDate? = null,
    val weekEvents: List<CalendarEvent> = emptyList(),
    val upcoming: List<CalendarEvent> = emptyList(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val busyEventIds: Set<Long> = emptySet(),
    val cancellationNotifications: List<CancellationNotification> = emptyList(),
    val cancellationNotificationError: String? = null,
    val acknowledgingCancellationIds: Set<Long> = emptySet(),
    val eventNotifications: EventNotifications = EventNotifications(emptyList(), emptyList()),
    val eventNotificationError: String? = null,
    val busyEventRequestIds: Set<Long> = emptySet(),
    val acknowledgingEventResponseIds: Set<Long> = emptySet(),
) {
    fun findEvent(id: Long): CalendarEvent? =
        weekEvents.firstOrNull { it.id == id } ?: upcoming.firstOrNull { it.id == id }

    private fun matchesRoom(event: CalendarEvent): Boolean = roomId == null || event.roomId == roomId

    val visibleWeekEvents: List<CalendarEvent> get() = weekEvents.filter(::matchesRoom)
    val visibleUpcoming: List<CalendarEvent> get() = upcoming.filter(::matchesRoom)
}

data class ParticipantsState(
    val participants: List<Participant>? = null,
    val error: String? = null,
)

data class ManageUiState(
    val courses: List<Course> = emptyList(),
    val instructors: List<InstructorOption> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val rules: List<RecurringRule> = emptyList(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val formError: String? = null,
) {
    val activeRules: List<RecurringRule>
        get() = rules.filter { it.active }.sortedWith(compareBy({ it.weekday }, { it.startTime }))
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = AppSettings(application)
    private val repository = SchoolRepository(ApiClient(settings, PersistentCookieJar(application)), settings)

    private val _session = MutableStateFlow<SessionState>(SessionState.Checking)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _login = MutableStateFlow(LoginUiState(lastEmail = settings.lastEmail, baseUrl = settings.baseUrl))
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _account = MutableStateFlow(AccountUiState())
    val account: StateFlow<AccountUiState> = _account.asStateFlow()

    private val _profile = MutableStateFlow(ProfileUiState())
    val profile: StateFlow<ProfileUiState> = _profile.asStateFlow()

    private val _calendar = MutableStateFlow(CalendarUiState())
    val calendar: StateFlow<CalendarUiState> = _calendar.asStateFlow()

    private val _participants = MutableStateFlow<Map<Long, ParticipantsState>>(emptyMap())
    val participants: StateFlow<Map<Long, ParticipantsState>> = _participants.asStateFlow()

    private val _manage = MutableStateFlow(ManageUiState())
    val manage: StateFlow<ManageUiState> = _manage.asStateFlow()

    private val _performance = MutableStateFlow(PerformanceUiState())
    val performance: StateFlow<PerformanceUiState> = _performance.asStateFlow()
    private val performanceMutex = Mutex()
    private var performanceSession = 0L
    private var pendingRequestVersion = 0L

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val baseUrl: String get() = repository.baseUrl

    private val currentUser: SessionUser? get() = (_session.value as? SessionState.LoggedIn)?.user

    init {
        restoreSession()
    }

    // ---------- Sesión ----------

    private fun restoreSession() {
        viewModelScope.launch {
            val user = try {
                repository.currentSession()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _login.update { it.copy(error = e.userMessage) }
                null
            }
            if (user != null) onLoggedIn(user) else _session.value = SessionState.LoggedOut
        }
    }

    fun login(email: String, password: String, baseUrl: String) {
        if (email.isBlank() || password.isBlank()) {
            _login.update { it.copy(error = "Introduce tu correo y contraseña.") }
            return
        }
        repository.updateBaseUrl(baseUrl)
        _login.update { it.copy(isLoading = true, error = null, notice = null, baseUrl = repository.baseUrl) }
        viewModelScope.launch {
            try {
                val user = repository.login(email, password)
                _login.update { it.copy(isLoading = false, lastEmail = email.trim()) }
                onLoggedIn(user)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = if (e.isUnauthorized) "Correo o contraseña incorrectos." else e.userMessage
                _login.update { it.copy(isLoading = false, error = message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            onLoggedOut(null)
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (_account.value.isChangingPassword) return
        _account.value = AccountUiState(isChangingPassword = true)
        viewModelScope.launch {
            try {
                repository.changePassword(currentPassword, newPassword)
                repository.clearLocalSession()
                onLoggedOut(null, "Contraseña cambiada. Inicia sesión con tu nueva contraseña.")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.isUnauthorized && e.message != "La contraseña actual no es correcta.") {
                    repository.clearLocalSession()
                    onLoggedOut("Tu sesión ha caducado. Vuelve a iniciar sesión.")
                } else {
                    _account.update { it.copy(isChangingPassword = false, passwordError = e.userMessage) }
                }
            }
        }
    }

    private fun onLoggedIn(user: SessionUser) {
        resetData()
        _session.value = SessionState.LoggedIn(user)
        viewModelScope.launch { loadAll(user) }
    }

    private fun onLoggedOut(message: String?, notice: String? = null) {
        _session.value = SessionState.LoggedOut
        resetData()
        _account.value = AccountUiState()
        _login.update { it.copy(isLoading = false, error = message, notice = notice, baseUrl = repository.baseUrl) }
    }

    private fun resetData() {
        _account.value = AccountUiState()
        performanceSession++
        pendingRequestVersion++
        _performance.value = PerformanceUiState()
        _profile.value = ProfileUiState()
        _calendar.value = CalendarUiState()
        _participants.value = emptyMap()
        _manage.value = ManageUiState()
    }

    fun refresh() {
        val user = currentUser ?: return
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                _participants.value = emptyMap()
                loadAll(user)
                if (_manage.value.loaded) loadManageData(force = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun loadAll(user: SessionUser) = coroutineScope {
        val profileJob = if (user.hasProfile) async { loadProfile() } else null
        val promotionNotificationsJob = if (user.hasProfile) async { refreshPromotionNotifications() } else null
        val cancellationNotificationsJob = if (user.canParticipate) async { refreshCancellationNotifications() } else null
        val eventNotificationsJob = async { refreshEventNotifications() }
        val roomsJob = async { loadRooms() }
        val performanceJob = async {
            if (_performance.value.loaded) loadPerformanceData() else refreshPerformanceRequests()
        }
        loadEvents()
        profileJob?.await()
        promotionNotificationsJob?.await()
        cancellationNotificationsJob?.await()
        eventNotificationsJob.await()
        roomsJob.await()
        performanceJob.await()
    }

    suspend fun refreshNotifications() = coroutineScope {
        val user = currentUser ?: return@coroutineScope
        val promotionJob = if (user.hasProfile) async { refreshPromotionNotifications() } else null
        val cancellationJob = if (user.canParticipate) async { refreshCancellationNotifications() } else null
        val eventNotificationsJob = async { refreshEventNotifications() }
        promotionJob?.await()
        cancellationJob?.await()
        eventNotificationsJob.await()
    }

    private suspend fun refreshPromotionNotifications() {
        try {
            val notices = repository.promotionNotifications()
            _profile.update { it.copy(promotionNotifications = notices, promotionNotificationError = null) }
        } catch (e: Exception) {
            val message = handleError(e) ?: return
            _profile.update { it.copy(promotionNotificationError = message) }
        }
    }

    private suspend fun refreshCancellationNotifications() {
        try {
            val notices = repository.cancellationNotifications()
            _calendar.update { it.copy(cancellationNotifications = notices, cancellationNotificationError = null) }
        } catch (e: Exception) {
            val message = handleError(e) ?: return
            _calendar.update { it.copy(cancellationNotificationError = message) }
        }
    }

    private suspend fun refreshEventNotifications() {
        try {
            val notices = repository.eventNotifications()
            _calendar.update { it.copy(eventNotifications = notices, eventNotificationError = null) }
        } catch (e: Exception) {
            val message = handleError(e) ?: return
            _calendar.update { it.copy(eventNotificationError = message) }
        }
    }

    fun respondToEventRequest(eventId: Long, accepted: Boolean) {
        if (eventId in _calendar.value.busyEventRequestIds) return
        _calendar.update { it.copy(busyEventRequestIds = it.busyEventRequestIds + eventId) }
        viewModelScope.launch {
            try {
                repository.respondToEventRequest(eventId, accepted)
                refreshEventNotifications()
                loadEvents()
                _messages.send(if (accepted) "Actividad aceptada." else "Actividad rechazada y eliminada.")
            } catch (e: Exception) {
                handleError(e)?.let { message -> _calendar.update { it.copy(eventNotificationError = message) } }
            } finally {
                _calendar.update { it.copy(busyEventRequestIds = it.busyEventRequestIds - eventId) }
            }
        }
    }

    fun acknowledgeEventResponse(noticeId: Long) {
        if (noticeId in _calendar.value.acknowledgingEventResponseIds) return
        _calendar.update { it.copy(acknowledgingEventResponseIds = it.acknowledgingEventResponseIds + noticeId) }
        viewModelScope.launch {
            try {
                repository.acknowledgeEventResponse(noticeId)
                _calendar.update { state ->
                    state.copy(
                        eventNotifications = state.eventNotifications.copy(
                            responses = state.eventNotifications.responses.filterNot { it.id == noticeId },
                        ),
                        eventNotificationError = null,
                    )
                }
            } catch (e: Exception) {
                handleError(e)?.let { message -> _calendar.update { it.copy(eventNotificationError = message) } }
            } finally {
                _calendar.update { it.copy(acknowledgingEventResponseIds = it.acknowledgingEventResponseIds - noticeId) }
            }
        }
    }

    fun acknowledgePromotionNotification(noticeId: Long) {
        if (noticeId in _profile.value.acknowledgingPromotionIds) return
        _profile.update { it.copy(acknowledgingPromotionIds = it.acknowledgingPromotionIds + noticeId) }
        viewModelScope.launch {
            try {
                repository.acknowledgePromotionNotification(noticeId)
                _profile.update { state -> state.copy(
                    promotionNotifications = state.promotionNotifications.filterNot { it.id == noticeId },
                    promotionNotificationError = null,
                ) }
            } catch (e: Exception) {
                handleError(e)?.let { message -> _profile.update { it.copy(promotionNotificationError = message) } }
            } finally {
                _profile.update { it.copy(acknowledgingPromotionIds = it.acknowledgingPromotionIds - noticeId) }
            }
        }
    }

    fun acknowledgeCancellationNotification(noticeId: Long) {
        if (noticeId in _calendar.value.acknowledgingCancellationIds) return
        _calendar.update { it.copy(acknowledgingCancellationIds = it.acknowledgingCancellationIds + noticeId) }
        viewModelScope.launch {
            try {
                repository.acknowledgeCancellationNotification(noticeId)
                _calendar.update { state -> state.copy(
                    cancellationNotifications = state.cancellationNotifications.filterNot { it.id == noticeId },
                    cancellationNotificationError = null,
                ) }
            } catch (e: Exception) {
                handleError(e)?.let { message -> _calendar.update { it.copy(cancellationNotificationError = message) } }
            } finally {
                _calendar.update { it.copy(acknowledgingCancellationIds = it.acknowledgingCancellationIds - noticeId) }
            }
        }
    }

    suspend fun refreshPerformanceRequests() {
        if (currentUser == null) return
        val session = performanceSession
        val version = ++pendingRequestVersion
        try {
            val pending = repository.pendingPerformanceRequests()
            if (session == performanceSession && version == pendingRequestVersion) {
                _performance.update { it.copy(inbox = pending, requestsError = null) }
            }
        } catch (e: Exception) {
            if (session != performanceSession || version != pendingRequestVersion) return
            val message = handleError(e) ?: return
            _performance.update { it.copy(requestsError = message) }
        }
    }

    fun reloadPerformance() {
        viewModelScope.launch { loadPerformanceData() }
    }

    private suspend fun loadPerformanceData() = performanceMutex.withLock {
        val user = currentUser ?: return@withLock
        val session = performanceSession
        _performance.update { it.copy(isLoading = true, error = null) }
        try {
            coroutineScope {
                val records = async { repository.performanceRecords() }
                val rankings = async { repository.performanceRankings() }
                val forms = async { if (user.hasProfile) repository.performanceForms() else emptyList() }
                val confirmers = async { if (user.hasProfile) repository.performanceConfirmers() else emptyList() }
                val loadedRecords = records.await()
                val loadedRankings = rankings.await()
                val loadedForms = forms.await()
                val loadedConfirmers = confirmers.await()
                if (session == performanceSession) {
                    _performance.update { it.copy(
                        records = loadedRecords, rankings = loadedRankings,
                        forms = loadedForms, confirmers = loadedConfirmers, loaded = true, isLoading = false,
                    ) }
                    refreshPerformanceRequests()
                }
            }
        } catch (e: Exception) {
            if (session != performanceSession) return@withLock
            _performance.update { it.copy(isLoading = false) }
            val message = handleError(e) ?: return@withLock
            _performance.update { it.copy(error = message) }
        }
    }

    fun savePerformance(payload: PerformancePayload) {
        val user = currentUser ?: return
        if (!user.hasProfile || _performance.value.isSaving) return
        val confirmers = payload.confirmerIds.distinct()
        if (confirmers.size !in 2..100 || user.studentId in confirmers || payload.formIds.distinct().size > 30) {
            _performance.update { it.copy(formError = "Selecciona de 2 a 100 confirmantes distintos de ti y un máximo de 30 formas.") }
            return
        }
        val session = performanceSession
        _performance.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            try {
                repository.savePerformance(payload)
                if (session != performanceSession) return@launch
                loadPerformanceData()
                _messages.send("Desempeño guardado. Se han enviado las solicitudes de confirmación.")
            } catch (e: Exception) {
                if (session != performanceSession) return@launch
                val message = handleError(e) ?: return@launch
                _performance.update { it.copy(formError = message) }
            } finally {
                if (session == performanceSession) _performance.update { it.copy(isSaving = false) }
            }
        }
    }

    fun respondToPerformance(recordId: Long, decision: PerformanceDecision) {
        if (recordId in _performance.value.busyRecordIds) return
        val user = currentUser ?: return
        val session = performanceSession
        _performance.update { it.copy(busyRecordIds = it.busyRecordIds + recordId) }
        viewModelScope.launch {
            try {
                repository.respondToPerformance(recordId, decision)
                if (session != performanceSession) return@launch
                pendingRequestVersion++
                _performance.update { it.copy(inbox = it.inbox.filterNot { request -> request.id == recordId }) }
                val message = if (user.isAdmin) {
                    if (decision == PerformanceDecision.CONFIRMED) "Aprobado por el administrador." else "Rechazado por el administrador."
                } else {
                    if (decision == PerformanceDecision.CONFIRMED) "Confirmación registrada." else "Has indicado que no puedes confirmar esta actividad."
                }
                _messages.send(message)
                loadPerformanceData()
            } catch (e: Exception) {
                if (session != performanceSession) return@launch
                handleError(e)?.let { _messages.send(it) }
                refreshPerformanceRequests()
            } finally {
                if (session == performanceSession) _performance.update { it.copy(busyRecordIds = it.busyRecordIds - recordId) }
            }
        }
    }

    // ---------- Mi ficha ----------

    private suspend fun loadProfile() {
        _profile.update { it.copy(isLoading = true, error = null) }
        try {
            val profile = repository.profile()
            _profile.update { it.copy(profile = profile, isLoading = false) }
        } catch (e: Exception) {
            _profile.update { it.copy(isLoading = false) }
            val message = handleError(e) ?: return
            _profile.update { it.copy(error = message) }
        }
    }

    fun reloadProfile() {
        viewModelScope.launch { loadProfile() }
    }

    fun saveFormProgress(formId: Long, percent: Int, formName: String) = formAction(formId) {
        repository.saveFormProgress(formId, percent)
        "Avance de «$formName» guardado: $percent %."
    }

    fun startLearning(formId: Long, formName: String) = formAction(formId) {
        repository.saveFormProgress(formId, 0)
        "Has marcado «$formName» como forma en aprendizaje."
    }

    fun stopLearning(formId: Long, formName: String) = formAction(formId) {
        repository.removeFormProgress(formId)
        "Has dejado de aprender «$formName»."
    }

    private fun formAction(formId: Long, action: suspend () -> String) {
        _profile.update { it.copy(pendingFormIds = it.pendingFormIds + formId) }
        viewModelScope.launch {
            try {
                val message = action()
                val profile = repository.profile()
                _profile.update { it.copy(profile = profile) }
                _messages.send(message)
            } catch (e: Exception) {
                handleError(e)?.let { _messages.send(it) }
            } finally {
                _profile.update { it.copy(pendingFormIds = it.pendingFormIds - formId) }
            }
        }
    }

    // ---------- Calendario ----------

    private suspend fun loadRooms() {
        try {
            val rooms = repository.rooms()
            _calendar.update { it.copy(rooms = rooms) }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private suspend fun loadEvents() {
        val weekStart = _calendar.value.selectedDate.weekStart()
        val today = schoolToday()
        _calendar.update { it.copy(isLoading = true, error = null) }
        try {
            coroutineScope {
                val week = async { repository.events(weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay()) }
                val upcoming = async { repository.events(today.atStartOfDay(), today.plusDays(UPCOMING_DAYS).atStartOfDay()) }
                val weekEvents = week.await()
                val upcomingEvents = upcoming.await()
                _calendar.update {
                    it.copy(
                        weekStart = weekStart,
                        weekEvents = weekEvents,
                        upcoming = upcomingEvents,
                        loaded = true,
                        isLoading = false,
                    )
                }
            }
        } catch (e: Exception) {
            _calendar.update { it.copy(isLoading = false) }
            val message = handleError(e) ?: return
            _calendar.update { it.copy(error = message) }
        }
    }

    fun reloadEvents() {
        viewModelScope.launch { loadEvents() }
    }

    fun selectDate(date: LocalDate) {
        val loadedWeek = _calendar.value.weekStart
        _calendar.update { it.copy(selectedDate = date) }
        if (date.weekStart() != loadedWeek) reloadWeek()
    }

    private fun reloadWeek() {
        val weekStart = _calendar.value.selectedDate.weekStart()
        _calendar.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val events = repository.events(weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay())
                if (_calendar.value.selectedDate.weekStart() == weekStart) {
                    _calendar.update { it.copy(weekStart = weekStart, weekEvents = events, isLoading = false) }
                }
            } catch (e: Exception) {
                _calendar.update { it.copy(isLoading = false) }
                val message = handleError(e) ?: return@launch
                _calendar.update { it.copy(error = message) }
            }
        }
    }

    fun setCalendarMode(mode: CalendarMode) = _calendar.update { it.copy(mode = mode) }

    fun setRoomFilter(roomId: Long?) = _calendar.update { it.copy(roomId = roomId) }

    // ---------- Participación ----------

    fun join(event: CalendarEvent, scope: ParticipationScope, role: ParticipationRole) = eventAction(event.id) {
        val effectiveScope = if (event.isRecurring) scope else ParticipationScope.OCCURRENCE
        val result = repository.join(event.id, effectiveScope, role)
        joinMessage(result, role, effectiveScope, event.isRecurring)
    }

    fun leave(event: CalendarEvent, scope: ParticipationScope) = eventAction(event.id) {
        repository.leave(event.id, scope)
        leaveMessage(scope)
    }

    private fun eventAction(eventId: Long, action: suspend () -> String) {
        _calendar.update { it.copy(busyEventIds = it.busyEventIds + eventId) }
        viewModelScope.launch {
            try {
                val message = action()
                loadEvents()
                if (_participants.value.containsKey(eventId)) loadParticipants(eventId)
                _messages.send(message)
            } catch (e: Exception) {
                handleError(e)?.let { _messages.send(it) }
            } finally {
                _calendar.update { it.copy(busyEventIds = it.busyEventIds - eventId) }
            }
        }
    }

    fun loadParticipants(eventId: Long) {
        viewModelScope.launch {
            try {
                val list = repository.participants(eventId)
                _participants.update { it + (eventId to ParticipantsState(participants = list)) }
            } catch (e: Exception) {
                val message = handleError(e) ?: return@launch
                _participants.update { it + (eventId to ParticipantsState(error = message)) }
            }
        }
    }

    // ---------- Gestión de agenda ----------

    fun loadManageData(force: Boolean = false) {
        val state = _manage.value
        if (state.isLoading || (state.loaded && !force)) return
        _manage.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val courses = async { repository.courses() }
                    val instructors = async { repository.instructors() }
                    val rooms = async { repository.rooms() }
                    val rules = async { repository.recurringRules() }
                    _manage.update {
                        it.copy(
                            courses = courses.await(),
                            instructors = instructors.await(),
                            rooms = rooms.await(),
                            rules = rules.await(),
                            loaded = true,
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                _manage.update { it.copy(isLoading = false) }
                val message = handleError(e) ?: return@launch
                _manage.update { it.copy(error = message) }
            }
        }
    }

    fun clearFormError() = _manage.update { it.copy(formError = null) }

    fun saveEvent(eventId: Long?, payload: EventPayload, onDone: () -> Unit) = manageAction(onDone) {
        if (eventId == null) {
            repository.createEvent(payload)
            _calendar.update { it.copy(selectedDate = payload.startAt.toLocalDate(), mode = CalendarMode.WEEK) }
            "Sesión programada."
        } else {
            repository.updateEvent(eventId, payload)
            "Sesión actualizada."
        }
    }

    fun cancelEvent(eventId: Long, onDone: () -> Unit) = manageAction(onDone) {
        repository.cancelEvent(eventId)
        "Sesión cancelada. Seguirá visible como cancelada."
    }

    fun deleteEvent(eventId: Long, onDeleted: () -> Unit) {
        _calendar.update { it.copy(busyEventIds = it.busyEventIds + eventId) }
        viewModelScope.launch {
            try {
                repository.deleteEvent(eventId)
                _participants.update { it - eventId }
                loadEvents()
                onDeleted()
                _messages.send("Actividad eliminada.")
            } catch (e: Exception) {
                handleError(e)?.let { _messages.send(it) }
            } finally {
                _calendar.update { it.copy(busyEventIds = it.busyEventIds - eventId) }
            }
        }
    }

    fun saveRule(ruleId: Long?, payload: RulePayload, onDone: () -> Unit) = manageAction(onDone) {
        if (ruleId == null) {
            repository.createRule(payload)
            "Horario habitual creado."
        } else {
            repository.updateRule(ruleId, payload)
            "Horario habitual actualizado."
        }
    }

    fun cancelRule(ruleId: Long, onDone: () -> Unit = {}) = manageAction(onDone) {
        repository.cancelRule(ruleId)
        "Horario habitual cancelado y sesiones futuras actualizadas."
    }

    private fun manageAction(onDone: () -> Unit, action: suspend () -> String) {
        if (_manage.value.isSaving) return
        _manage.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            try {
                val message = action()
                _manage.update { it.copy(isSaving = false) }
                onDone()
                _messages.send(message)
                _participants.value = emptyMap()
                loadEvents()
                loadManageData(force = true)
            } catch (e: Exception) {
                _manage.update { it.copy(isSaving = false) }
                val message = handleError(e) ?: return@launch
                _manage.update { it.copy(formError = message) }
            }
        }
    }

    /** Cierra la sesión en un 401; si no, devuelve el mensaje que debe mostrarse. */
    private fun handleError(e: Exception): String? {
        if (e is CancellationException) throw e
        if (e.isUnauthorized) {
            onLoggedOut("Tu sesión ha caducado. Vuelve a iniciar sesión.")
            return null
        }
        return e.userMessage
    }
}

private val Exception.isUnauthorized: Boolean
    get() = (this as? ApiException)?.isUnauthorized == true

private val Exception.userMessage: String
    get() = when (this) {
        is ApiException -> calendarErrorText(status, message ?: "Error inesperado.")
        else -> "Respuesta inesperada del servidor."
    }
