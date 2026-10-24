package com.example.kungfuapplication.data

import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.Course
import com.example.kungfuapplication.data.model.EventPayload
import com.example.kungfuapplication.data.model.InstructorOption
import com.example.kungfuapplication.data.model.JoinResult
import com.example.kungfuapplication.data.model.MyProfile
import com.example.kungfuapplication.data.model.Participant
import com.example.kungfuapplication.data.model.ParticipationRole
import com.example.kungfuapplication.data.model.ParticipationScope
import com.example.kungfuapplication.data.model.RecurringRule
import com.example.kungfuapplication.data.model.Room
import com.example.kungfuapplication.data.model.RulePayload
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.remote.ApiClient
import com.example.kungfuapplication.data.remote.ApiException
import com.example.kungfuapplication.data.remote.asObject
import com.example.kungfuapplication.data.remote.parseCourse
import com.example.kungfuapplication.data.remote.parseEvent
import com.example.kungfuapplication.data.remote.parseInstructor
import com.example.kungfuapplication.data.remote.parseJoinResult
import com.example.kungfuapplication.data.remote.parseParticipant
import com.example.kungfuapplication.data.remote.parseProfile
import com.example.kungfuapplication.data.remote.parseRoom
import com.example.kungfuapplication.data.remote.parseRule
import com.example.kungfuapplication.data.remote.parseSessionUser
import com.example.kungfuapplication.data.remote.toApiDateTime
import com.example.kungfuapplication.data.remote.toJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime

/** Acceso a la API del backend con los mismos endpoints que usa la web. */
class SchoolRepository(
    private val api: ApiClient,
    private val settings: AppSettings,
) {
    val baseUrl: String get() = api.baseUrl

    // ---------- Autenticación ----------

    suspend fun login(email: String, password: String): SessionUser {
        val body = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }
        val user = parseSessionUser(api.post("/api/auth/login", body).asObject())
        settings.lastEmail = email.trim()
        return user
    }

    /** Devuelve el usuario de la sesión guardada, o null si no hay sesión activa en el servidor. */
    suspend fun currentSession(): SessionUser? = try {
        parseSessionUser(api.get("/api/auth/me").asObject())
    } catch (e: ApiException) {
        if (e.isUnauthorized) null else throw e
    }

    suspend fun logout() {
        try {
            api.delete("/api/auth/logout")
        } finally {
            api.clearSession()
        }
    }

    fun updateBaseUrl(url: String) {
        val normalized = AppSettings.normalizeBaseUrl(url)
        if (normalized != settings.baseUrl) {
            api.clearSession()
            settings.baseUrl = normalized
        }
    }

    // ---------- Mi ficha ----------

    suspend fun profile(): MyProfile = parseProfile(api.get("/api/me/profile").asObject())

    suspend fun saveFormProgress(formId: Long, percent: Int) {
        api.put("/api/me/forms/$formId/progress", buildJsonObject { put("percent", percent) })
    }

    suspend fun removeFormProgress(formId: Long) {
        api.delete("/api/me/forms/$formId/progress")
    }

    // ---------- Agenda ----------

    suspend fun events(from: LocalDateTime, to: LocalDateTime): List<CalendarEvent> {
        val query = "from=${api.encode(from.toApiDateTime())}&to=${api.encode(to.toApiDateTime())}"
        return api.getArray("/api/calendar/events?$query").map(::parseEvent).sortedBy { it.start }
    }

    suspend fun rooms(): List<Room> = api.getArray("/api/calendar/rooms").map(::parseRoom)

    suspend fun courses(): List<Course> = api.getArray("/api/calendar/courses").map(::parseCourse)

    suspend fun instructors(): List<InstructorOption> = api.getArray("/api/calendar/instructors").map(::parseInstructor)

    suspend fun recurringRules(): List<RecurringRule> = api.getArray("/api/calendar/recurring-rules").map(::parseRule)

    suspend fun join(eventId: Long, scope: ParticipationScope, role: ParticipationRole): JoinResult {
        val body = buildJsonObject {
            put("scope", scope.apiValue)
            put("role", role.name)
        }
        return parseJoinResult(api.post("/api/calendar/events/$eventId/participation", body).asObject())
    }

    suspend fun leave(eventId: Long, scope: ParticipationScope) {
        api.delete("/api/calendar/events/$eventId/participation?scope=${scope.apiValue}")
    }

    suspend fun participants(eventId: Long): List<Participant> =
        api.getArray("/api/calendar/events/$eventId/participants").map(::parseParticipant)

    suspend fun createEvent(payload: EventPayload) {
        api.post("/api/calendar/events", payload.toJson())
    }

    suspend fun updateEvent(eventId: Long, payload: EventPayload) {
        api.patch("/api/calendar/events/$eventId", payload.toJson())
    }

    suspend fun cancelEvent(eventId: Long) {
        api.post("/api/calendar/events/$eventId/cancel")
    }

    suspend fun createRule(payload: RulePayload) {
        api.post("/api/calendar/recurring-rules", payload.toJson())
    }

    suspend fun updateRule(ruleId: Long, payload: RulePayload) {
        api.patch("/api/calendar/recurring-rules/$ruleId", payload.toJson())
    }

    suspend fun cancelRule(ruleId: Long) {
        api.post("/api/calendar/recurring-rules/$ruleId/cancel")
    }
}
