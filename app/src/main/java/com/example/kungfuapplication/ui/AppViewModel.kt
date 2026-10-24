package com.example.kungfuapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kungfuapplication.data.AppSettings
import com.example.kungfuapplication.data.SchoolRepository
import com.example.kungfuapplication.data.model.CalendarEvent
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
    val lastEmail: String = "",
    val baseUrl: String = AppSettings.DEFAULT_BASE_URL,
)

data class ProfileUiState(
    val profile: MyProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingFormIds: Set<Long> = emptySet(),
)

enum class CalendarMode { WEEK, LIST }

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

    private val _profile = MutableStateFlow(ProfileUiState())
    val profile: StateFlow<ProfileUiState> = _profile.asStateFlow()

    private val _calendar = MutableStateFlow(CalendarUiState())
    val calendar: StateFlow<CalendarUiState> = _calendar.asStateFlow()

    private val _participants = MutableStateFlow<Map<Long, ParticipantsState>>(emptyMap())
    val participants: StateFlow<Map<Long, ParticipantsState>> = _participants.asStateFlow()

    private val _manage = MutableStateFlow(ManageUiState())
    val manage: StateFlow<ManageUiState> = _manage.asStateFlow()

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
        _login.update { it.copy(isLoading = true, error = null, baseUrl = repository.baseUrl) }
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

    private fun onLoggedIn(user: SessionUser) {
        resetData()
        _session.value = SessionState.LoggedIn(user)
        viewModelScope.launch { loadAll(user) }
    }

    private fun onLoggedOut(message: String?) {
        _session.value = SessionState.LoggedOut
        resetData()
        _login.update { it.copy(isLoading = false, error = message, baseUrl = repository.baseUrl) }
    }

    private fun resetData() {
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
        val roomsJob = async { loadRooms() }
        loadEvents()
        profileJob?.await()
        roomsJob.await()
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
