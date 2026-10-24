package com.example.kungfuapplication

import com.example.kungfuapplication.data.model.Attendees
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.CourseType
import com.example.kungfuapplication.data.model.FormState
import com.example.kungfuapplication.data.model.JoinResult
import com.example.kungfuapplication.data.model.ParticipationRole
import com.example.kungfuapplication.data.model.ParticipationScope
import com.example.kungfuapplication.data.model.ParticipationSettings
import com.example.kungfuapplication.data.model.RulePayload
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.UserRole
import com.example.kungfuapplication.data.model.attendanceSummary
import com.example.kungfuapplication.data.model.calendarErrorText
import com.example.kungfuapplication.data.model.isSeatBlocked
import com.example.kungfuapplication.data.model.joinMessage
import com.example.kungfuapplication.data.model.myParticipationText
import com.example.kungfuapplication.data.model.state
import com.example.kungfuapplication.data.remote.parseEvent
import com.example.kungfuapplication.data.remote.parseProfile
import com.example.kungfuapplication.data.remote.parseRule
import com.example.kungfuapplication.data.remote.parseSessionUser
import com.example.kungfuapplication.data.remote.toJson
import com.example.kungfuapplication.ui.calendar.FormCheck
import com.example.kungfuapplication.ui.calendar.checkEvent
import com.example.kungfuapplication.ui.calendar.checkLimit
import com.example.kungfuapplication.ui.calendar.checkRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BackendMappingTest {

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun event(
        limit: Int? = null,
        attendees: Attendees? = Attendees(3, 1),
        full: Boolean = false,
        ruleId: Long? = null,
    ) = CalendarEvent(
        id = 1, courseId = 1, instructorId = 1, roomId = 1,
        start = LocalDateTime.of(2026, 5, 4, 18, 0), end = LocalDateTime.of(2026, 5, 4, 19, 0),
        active = true, recurringRuleId = ruleId, modifiedManually = false,
        courseName = "Kung Fu", courseTopic = null, courseDescription = null, courseType = CourseType.HABITUAL,
        instructorName = null, roomName = null, participantLimit = limit, showAttendeeCount = attendees != null,
        attendees = attendees, full = full, myParticipation = null,
    )

    @Test
    fun parsesEventWithStringIdsAndParticipation() {
        val parsed = parseEvent(
            json(
                """
                {"id_curso_agendado":"42","id_curso":"3","id_instructor":"7","id_sala":"2",
                 "fecha_hora":"2026-05-04T18:00:00","fecha_hora_fin":"2026-05-04T19:30:00","activo":true,
                 "id_horario_recurrente":"9","modificado_manualmente":false,"curso_nombre":"Taolu",
                 "tipo_curso":"CURSO_ESPECIAL","limite_participantes":10,"mostrar_asistentes":true,
                 "asistentes":{"alumnos":4,"instructores":1},"completo":false,
                 "mi_participacion":{"rol":"INSTRUCTOR","serie":true}}
                """,
            ),
        )
        assertEquals(42L, parsed.id)
        assertEquals(LocalDateTime.of(2026, 5, 4, 18, 0), parsed.start)
        assertEquals(LocalDateTime.of(2026, 5, 4, 19, 30), parsed.end)
        assertTrue(parsed.isRecurring)
        assertEquals(CourseType.CURSO_ESPECIAL, parsed.courseType)
        assertEquals(Attendees(4, 1), parsed.attendees)
        assertEquals(ParticipationRole.INSTRUCTOR, parsed.myParticipation?.role)
        assertEquals("Estás apuntado como instructor a toda la serie.", parsed.myParticipationText())
    }

    @Test
    fun parsesEventWithHiddenAttendees() {
        val parsed = parseEvent(
            json("""{"id_curso_agendado":1,"id_curso":1,"fecha_hora":"2026-05-04T18:00:00","asistentes":null,"mi_participacion":null,"limite_participantes":null,"completo":true}"""),
        )
        assertNull(parsed.attendees)
        assertNull(parsed.myParticipation)
        assertNull(parsed.participantLimit)
        assertEquals(LocalDateTime.of(2026, 5, 4, 19, 0), parsed.end)
        assertEquals("Plazas completas", parsed.attendanceSummary())
    }

    @Test
    fun parsesProfileAndFormStates() {
        val profile = parseProfile(
            json(
                """
                {"student":{"id":5,"name":"Marc Soler","active":true,"isInstructor":false,"birthDate":"2001-02-03"},
                 "grade":{"id":2,"name":"Cinta Naranja","beltColor":"#FFA500","order":2,"requiredForms":2},
                 "knownForms":[{"id":1,"name":"Wu Bu Quan","style":"Changquan","learnedOn":"2025-01-10"}],
                 "gradeForms":[
                   {"id":1,"name":"Wu Bu Quan","optional":false,"known":true,"learning":null},
                   {"id":2,"name":"Lian Huan Quan","optional":false,"known":false,"learning":{"percent":40,"updatedOn":"2026-01-01"}},
                   {"id":3,"name":"Gong Li Quan","optional":true,"known":false,"learning":null}],
                 "instructor":null}
                """,
            ),
        )
        assertEquals("Marc Soler", profile.student.name)
        assertEquals(LocalDate.of(2001, 2, 3), profile.student.birthDate)
        assertEquals(listOf(FormState.KNOWN, FormState.LEARNING, FormState.PENDING), profile.gradeForms.map { it.state })
        assertEquals(2, profile.requiredForms.size)
        assertEquals(1, profile.optionalForms.size)
        assertEquals(1, profile.knownRequiredCount)
        assertEquals(1, profile.learningCount)
        assertEquals(40, profile.gradeForms[1].learning?.percent)
        assertNull(profile.instructor)
    }

    @Test
    fun parsesRuleTimes() {
        val rule = parseRule(
            json("""{"id_horario_recurrente":"3","id_curso":"1","dia_semana":2,"hora_inicio":"18:30:00","hora_fin":"20:00:00","fecha_inicio":"2026-01-05","fecha_fin":null,"activo":true}"""),
        )
        assertEquals(3L, rule.id)
        assertEquals(LocalTime.of(18, 30), rule.startTime)
        assertEquals(LocalTime.of(20, 0), rule.endTime)
        assertNull(rule.endsOn)
    }

    @Test
    fun sessionUserPermissions() {
        val admin = parseSessionUser(json("""{"role":"ADMIN","userId":1,"displayName":"Admin"}"""))
        val instructor = parseSessionUser(json("""{"role":"INSTRUCTOR","userId":2,"displayName":"Chen","studentId":"8","instructorId":"3"}"""))
        val student = parseSessionUser(json("""{"role":"USER","userId":3,"displayName":"Marc","studentId":5}"""))

        assertTrue(admin.canManageCalendar && admin.canEditParticipation)
        assertFalse(admin.canParticipate || admin.hasProfile)

        assertTrue(instructor.canManageCalendar && instructor.canParticipate && instructor.canChooseInstructorRole && instructor.hasProfile)
        assertFalse(instructor.canEditParticipation)
        assertEquals(3L, instructor.instructorId)

        assertTrue(student.canParticipate && student.hasProfile)
        assertFalse(student.canManageCalendar || student.canChooseInstructorRole)
        assertEquals(SessionUser(UserRole.USER, 3, "Marc", 5, null), student)
    }

    @Test
    fun attendanceSummaryMatchesWeb() {
        assertEquals("3 alumnos apuntados · 1 instructor · Sin límite de plazas", event().attendanceSummary())
        assertEquals("1 alumno apuntado · 0 instructores · Sin límite de plazas", event(attendees = Attendees(1, 0)).attendanceSummary())
        assertEquals("3 / 10 plazas ocupadas · 1 instructor", event(limit = 10).attendanceSummary())
        assertEquals("Sin límite de plazas", event(attendees = null).attendanceSummary())
        assertEquals("Quedan plazas libres", event(limit = 5, attendees = null).attendanceSummary())
    }

    @Test
    fun fullSessionBlocksOnlyStudentsForSingleDay() {
        val full = event(limit = 3, full = true, ruleId = 1)
        assertTrue(full.isSeatBlocked(ParticipationRole.ALUMNO, ParticipationScope.OCCURRENCE))
        assertFalse(full.isSeatBlocked(ParticipationRole.ALUMNO, ParticipationScope.SERIES))
        assertFalse(full.isSeatBlocked(ParticipationRole.INSTRUCTOR, ParticipationScope.OCCURRENCE))
        assertFalse(event(limit = 3).isSeatBlocked(ParticipationRole.ALUMNO, ParticipationScope.OCCURRENCE))
    }

    @Test
    fun joinMessages() {
        assertEquals(
            "Te has apuntado como alumno.",
            joinMessage(JoinResult(1, emptyList()), ParticipationRole.ALUMNO, ParticipationScope.OCCURRENCE, recurring = true),
        )
        val series = joinMessage(JoinResult(3, listOf(LocalDate.of(2026, 5, 11))), ParticipationRole.ALUMNO, ParticipationScope.SERIES, recurring = true)
        assertTrue(series, series.startsWith("Te has apuntado a 3 fechas de la serie. Sin plaza: 11 may"))
        assertEquals(
            "Te has apuntado a 1 fecha de la serie.",
            joinMessage(JoinResult(1, emptyList()), ParticipationRole.INSTRUCTOR, ParticipationScope.SERIES, recurring = true),
        )
    }

    @Test
    fun overlapErrorGetsRoomPrefix() {
        assertEquals("La sala ya está ocupada en ese horario. Se solapa con otra sesión", calendarErrorText(409, "Se solapa con otra sesión"))
        assertEquals("Otro error", calendarErrorText(409, "Otro error"))
        assertEquals("Se solapa", calendarErrorText(400, "Se solapa"))
    }

    @Test
    fun validatesLimit() {
        assertEquals(FormCheck.Valid(null), checkLimit(unlimited = true, limitText = "abc"))
        assertEquals(FormCheck.Valid(12), checkLimit(unlimited = false, limitText = " 12 "))
        assertTrue(checkLimit(unlimited = false, limitText = "0") is FormCheck.Invalid)
        assertTrue(checkLimit(unlimited = false, limitText = "") is FormCheck.Invalid)
    }

    @Test
    fun validatesEventAndRule() {
        val date = LocalDate.of(2026, 5, 4)
        val six = LocalTime.of(18, 0)
        val seven = LocalTime.of(19, 0)

        assertEquals(
            FormCheck.Invalid("Selecciona un curso, un instructor y una sala activa."),
            checkEvent(1, null, 1, date, six, seven, null),
        )
        assertEquals(FormCheck.Invalid("El final debe ser posterior al inicio."), checkEvent(1, 1, 1, date, seven, six, null))
        val valid = checkEvent(1, 2, 3, date, six, seven, ParticipationSettings(null, false))
        assertTrue(valid is FormCheck.Valid)
        val body = (valid as FormCheck.Valid).payload.toJson()
        assertEquals("2026-05-04T18:00", body["startAt"].toString().trim('"'))
        assertEquals(JsonNull, body["participantLimit"])
        assertEquals("false", body["showAttendeeCount"].toString())

        assertEquals(
            FormCheck.Invalid("Selecciona al menos un día de la semana."),
            checkRule(1, 1, 1, emptyList(), six, seven, date, null, null),
        )
        assertEquals(
            FormCheck.Invalid("La fecha final debe ser posterior a la inicial."),
            checkRule(1, 1, 1, listOf(1), six, seven, date, date.minusDays(1), null),
        )
        val rule = checkRule(1, 1, 1, listOf(5, 1), six, seven, date, null, null) as FormCheck.Valid<RulePayload>
        assertEquals(listOf(1, 5), rule.payload.weekdays)
        val ruleBody = rule.payload.toJson()
        assertFalse("instructor sin permisos de admin no envía ajustes", ruleBody.containsKey("participantLimit"))
        assertEquals("\"18:00\"", ruleBody["startTime"].toString())
    }
}
