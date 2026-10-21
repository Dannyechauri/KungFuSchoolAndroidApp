package com.example.kungfuapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kungfuapplication.data.AppSettings
import com.example.kungfuapplication.data.SchoolRepository
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.remote.ApiClient
import com.example.kungfuapplication.data.remote.ApiException
import com.example.kungfuapplication.data.remote.PersistentCookieJar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

data class DataUiState(
    val data: SchoolData? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val pendingClassIds: Set<Long> = emptySet(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = AppSettings(application)
    private val repository = SchoolRepository(ApiClient(settings, PersistentCookieJar(application)), settings)

    private val _session = MutableStateFlow<SessionState>(SessionState.Checking)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _login = MutableStateFlow(LoginUiState(lastEmail = settings.lastEmail, baseUrl = settings.baseUrl))
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _data = MutableStateFlow(DataUiState())
    val data: StateFlow<DataUiState> = _data.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val baseUrl: String get() = repository.baseUrl

    init {
        restoreSession()
    }

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

    fun refresh() = loadData(isRefresh = true)

    fun confirmAttendance(classIds: List<Long>) {
        val profile = _data.value.data?.profile ?: return
        launchClassAction(classIds) {
            var confirmed = 0
            var lastError: String? = null
            classIds.forEach { id ->
                try {
                    repository.confirmAttendance(profile, id)
                    confirmed++
                } catch (e: Exception) {
                    if (e is CancellationException || e.isUnauthorized) throw e
                    lastError = e.userMessage
                }
            }
            when {
                confirmed == 0 -> "No se pudo confirmar la asistencia. ${lastError.orEmpty()}".trim()
                classIds.size > 1 -> "Asistencia confirmada en $confirmed de ${classIds.size} clases."
                else -> "¡Asistencia confirmada!"
            }
        }
    }

    fun cancelAttendance(classId: Long, attendanceId: Long) {
        launchClassAction(listOf(classId)) {
            repository.cancelAttendance(attendanceId)
            "Asistencia cancelada."
        }
    }

    private fun launchClassAction(classIds: List<Long>, action: suspend () -> String) {
        _data.update { it.copy(pendingClassIds = it.pendingClassIds + classIds) }
        viewModelScope.launch {
            try {
                val message = action()
                val attendances = repository.loadAttendances()
                _data.update { state ->
                    state.copy(data = state.data?.copy(attendances = attendances))
                }
                _messages.send(message)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _data.update { it.copy(pendingClassIds = it.pendingClassIds - classIds.toSet()) }
            }
        }
    }

    private fun onLoggedIn(user: SessionUser) {
        _session.value = SessionState.LoggedIn(user)
        _data.value = DataUiState()
        loadData(isRefresh = false)
    }

    private fun onLoggedOut(message: String?) {
        _session.value = SessionState.LoggedOut
        _data.value = DataUiState()
        _login.update { it.copy(isLoading = false, error = message, baseUrl = repository.baseUrl) }
    }

    private fun loadData(isRefresh: Boolean) {
        val user = (_session.value as? SessionState.LoggedIn)?.user ?: return
        _data.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null) }
        viewModelScope.launch {
            try {
                val data = repository.loadSchoolData(user)
                _data.update { it.copy(data = data, isLoading = false, isRefreshing = false) }
            } catch (e: Exception) {
                _data.update { it.copy(isLoading = false, isRefreshing = false) }
                handleError(e)
            }
        }
    }

    private suspend fun handleError(e: Exception) {
        if (e is CancellationException) throw e
        if (e.isUnauthorized) {
            onLoggedOut("Tu sesión ha caducado. Vuelve a iniciar sesión.")
            return
        }
        val message = e.userMessage
        if (_data.value.data == null) {
            _data.update { it.copy(error = message) }
        } else {
            _messages.send(message)
        }
    }
}

private val Exception.isUnauthorized: Boolean
    get() = (this as? ApiException)?.isUnauthorized == true

private val Exception.userMessage: String
    get() = if (this is ApiException) message ?: "Error inesperado." else "Respuesta inesperada del servidor."
