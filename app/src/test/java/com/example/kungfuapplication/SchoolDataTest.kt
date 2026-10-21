package com.example.kungfuapplication

import com.example.kungfuapplication.data.model.Attendance
import com.example.kungfuapplication.data.model.Course
import com.example.kungfuapplication.data.model.FormStatus
import com.example.kungfuapplication.data.model.Grade
import com.example.kungfuapplication.data.model.GradeForm
import com.example.kungfuapplication.data.model.LearnedForm
import com.example.kungfuapplication.data.model.MartialForm
import com.example.kungfuapplication.data.model.PersonType
import com.example.kungfuapplication.data.model.ScheduledClass
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.Student
import com.example.kungfuapplication.data.model.UserProfile
import com.example.kungfuapplication.data.remote.date
import com.example.kungfuapplication.data.remote.dateTime
import com.example.kungfuapplication.data.remote.long
import com.example.kungfuapplication.data.remote.parseBackendDateTime
import com.example.kungfuapplication.data.remote.str
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

class SchoolDataTest {

    @Test
    fun parsesRowAsSerializedByBackend() {
        val row = Json.parseToJsonElement(
            """{"id_curso_agendado":7,"fecha_hora":"2026-10-21T16:30:00.000+00:00","fecha":"2026-01-05","activo":true,"nota":null}""",
        ).jsonObject
        val expected = OffsetDateTime.parse("2026-10-21T16:30:00Z")
            .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        assertEquals(7L, row.long("id_curso_agendado"))
        assertEquals(expected, row.dateTime("fecha_hora"))
        assertEquals(LocalDate.of(2026, 1, 5), row.date("fecha"))
        assertNull(row.str("nota"))
    }

    @Test
    fun parsesAlternativeDateTimeFormats() {
        val instant = OffsetDateTime.parse("2026-10-21T16:30:00Z")
        val local = instant.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        assertEquals(local, parseBackendDateTime(instant.toInstant().toEpochMilli().toString()))
        assertEquals(local, parseBackendDateTime("2026-10-21T16:30:00.000+0000"))
        assertEquals(LocalDateTime.of(2026, 10, 21, 16, 30), parseBackendDateTime("2026-10-21 16:30:00"))
        assertNull(parseBackendDateTime(""))
    }

    @Test
    fun formStatusAndProgressFollowGrades() {
        val data = sampleData()
        assertTrue(data.formStatus(form(1)) is FormStatus.Learned)
        assertEquals(FormStatus.Available, data.formStatus(form(2)))
        assertTrue(data.formStatus(form(3)) is FormStatus.Locked)
        val progress = data.nextGradeProgress!!
        assertEquals(2L, progress.nextGrade.id)
        assertEquals(0, progress.learned)
        assertEquals(1, progress.total)
    }

    @Test
    fun seriesIncludesSameCourseAndTimeFromSelectedDay() {
        val data = sampleData()
        val now = LocalDateTime.of(2026, 1, 1, 9, 0)
        val first = data.classItem(10)!!
        assertEquals(listOf(10L, 11L), data.seriesFrom(first, now).map { it.scheduled.id })
        assertEquals(listOf(11L), data.myUpcomingClasses(now).map { it.scheduled.id })
    }

    private fun form(id: Long) = MartialForm(id, 1, "Forma $id", null)

    private fun sampleData(): SchoolData {
        val student = Student(5, "Ana López", "M-1", null, true, null, gradeId = 1, email = "ana@x.com")
        val profile = UserProfile(SessionUser("USER", 5, "Ana López"), PersonType.STUDENT, student = student)
        val at = { day: Int, hour: Int -> LocalDateTime.of(2026, 1, day, hour, 0) }
        return SchoolData(
            profile = profile,
            grades = listOf(Grade(1, "Blanco", 1, "Blanco", 1), Grade(2, "Amarillo", 2, "Amarillo", 1)),
            styles = emptyList(),
            forms = listOf(form(1), form(2), form(3)),
            gradeForms = listOf(GradeForm(1, 1, false), GradeForm(1, 2, true), GradeForm(2, 3, false)),
            learnedForms = listOf(LearnedForm(5, 1, LocalDate.of(2025, 12, 1))),
            courses = listOf(Course(1, "Kung Fu básico", null, null), Course(2, "Taichí", null, null)),
            classes = listOf(
                ScheduledClass(10, 1, null, at(5, 18), true),
                ScheduledClass(11, 1, null, at(12, 18), true),
                ScheduledClass(12, 1, null, at(13, 19), true),
                ScheduledClass(13, 2, null, at(14, 18), true),
            ),
            instructors = emptyList(),
            students = listOf(student),
            attendances = listOf(Attendance(1, 11, 5, null, null, null)),
            enrollments = emptyList(),
            instructorStyles = emptyList(),
        )
    }
}
