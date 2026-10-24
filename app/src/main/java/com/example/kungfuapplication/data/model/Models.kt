package com.example.kungfuapplication.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class UserRole { ADMIN, INSTRUCTOR, USER }

data class SessionUser(
    val role: UserRole,
    val userId: Long?,
    val displayName: String,
    val studentId: Long?,
    val instructorId: Long?,
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN

    /** Igual que la web: admin e instructores gestionan la agenda. */
    val canManageCalendar: Boolean get() = role == UserRole.ADMIN || role == UserRole.INSTRUCTOR

    /** Solo el administrador cambia límites de plazas y la visibilidad del número de asistentes. */
    val canEditParticipation: Boolean get() = isAdmin

    /** Alumnos e instructores (que también son alumnos) se apuntan a las actividades. */
    val canParticipate: Boolean get() = role == UserRole.USER || role == UserRole.INSTRUCTOR

    val canChooseInstructorRole: Boolean get() = role == UserRole.INSTRUCTOR

    val hasProfile: Boolean get() = studentId != null && !isAdmin

    val roleLabel: String
        get() = when (role) {
            UserRole.ADMIN -> "Administración"
            UserRole.INSTRUCTOR -> "Instructor"
            UserRole.USER -> "Alumno"
        }
}

// ---------- Mi ficha ----------

data class MyProfile(
    val student: StudentInfo,
    val grade: GradeInfo?,
    val knownForms: List<KnownForm>,
    val gradeForms: List<GradeFormItem>,
    val instructor: InstructorInfo?,
) {
    val requiredForms: List<GradeFormItem> get() = gradeForms.filter { !it.optional }
    val optionalForms: List<GradeFormItem> get() = gradeForms.filter { it.optional }
    val knownRequiredCount: Int get() = requiredForms.count { it.known }
    val learningCount: Int get() = gradeForms.count { !it.known && it.learning != null }
}

data class StudentInfo(
    val id: Long,
    val name: String,
    val enrollmentNumber: String?,
    val email: String?,
    val birthDate: LocalDate?,
    val joinedOn: LocalDate?,
    val active: Boolean,
    val isInstructor: Boolean,
)

data class GradeInfo(
    val id: Long,
    val name: String,
    val beltColor: String?,
    val order: Int?,
    val requiredForms: Int?,
)

data class KnownForm(
    val id: Long,
    val name: String,
    val description: String?,
    val style: String?,
    val learnedOn: LocalDate?,
)

data class FormProgress(val percent: Int, val updatedOn: LocalDate?)

data class GradeFormItem(
    val id: Long,
    val name: String,
    val description: String?,
    val style: String?,
    val optional: Boolean,
    val known: Boolean,
    val learning: FormProgress?,
)

enum class FormState { KNOWN, LEARNING, PENDING }

val GradeFormItem.state: FormState
    get() = when {
        known -> FormState.KNOWN
        learning != null -> FormState.LEARNING
        else -> FormState.PENDING
    }

data class InstructorInfo(
    val id: Long,
    val specialty: String?,
    val hiredOn: LocalDate?,
    val active: Boolean,
    val styles: List<String>,
)

// ---------- Agenda ----------

enum class CourseType { HABITUAL, CURSO_ESPECIAL }

enum class ParticipationRole(val label: String) { ALUMNO("alumno"), INSTRUCTOR("instructor") }

enum class ParticipationScope(val apiValue: String) { OCCURRENCE("occurrence"), SERIES("series") }

data class Attendees(val students: Int, val instructors: Int)

data class MyParticipation(val role: ParticipationRole, val series: Boolean)

data class CalendarEvent(
    val id: Long,
    val courseId: Long,
    val instructorId: Long?,
    val roomId: Long?,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val active: Boolean,
    val recurringRuleId: Long?,
    val modifiedManually: Boolean,
    val courseName: String,
    val courseTopic: String?,
    val courseDescription: String?,
    val courseType: CourseType,
    val instructorName: String?,
    val roomName: String?,
    val participantLimit: Int?,
    val showAttendeeCount: Boolean,
    val attendees: Attendees?,
    val full: Boolean,
    val myParticipation: MyParticipation?,
) {
    val isRecurring: Boolean get() = recurringRuleId != null
    fun hasStarted(now: LocalDateTime): Boolean = !start.isAfter(now)
}

data class Room(val id: Long, val name: String, val active: Boolean)

data class Course(
    val id: Long,
    val name: String,
    val topic: String?,
    val description: String?,
    val type: CourseType,
)

data class InstructorOption(val id: Long, val name: String, val active: Boolean)

data class RecurringRule(
    val id: Long,
    val courseId: Long,
    val courseName: String,
    val courseType: CourseType,
    val instructorId: Long?,
    val instructorName: String?,
    val roomId: Long?,
    val roomName: String?,
    val weekday: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val startsOn: LocalDate,
    val endsOn: LocalDate?,
    val active: Boolean,
    val participantLimit: Int?,
    val showAttendeeCount: Boolean,
)

data class Participant(
    val studentId: Long,
    val name: String,
    val role: ParticipationRole,
    val series: Boolean,
)

data class JoinResult(val joined: Int, val skipped: List<LocalDate>)

/** Valores comunes de los formularios de sesión puntual y horario habitual. */
data class ParticipationSettings(val participantLimit: Int?, val showAttendeeCount: Boolean)

data class EventPayload(
    val courseId: Long,
    val instructorId: Long,
    val roomId: Long,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val settings: ParticipationSettings?,
)

data class RulePayload(
    val courseId: Long,
    val instructorId: Long,
    val roomId: Long,
    val weekdays: List<Int>,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val startsOn: LocalDate,
    val endsOn: LocalDate?,
    val settings: ParticipationSettings?,
)
