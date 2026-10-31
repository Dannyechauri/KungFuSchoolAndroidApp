package com.example.kungfuapplication.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

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

data class PromotionNotification(
    val id: Long,
    val previousGrade: String,
    val newGrade: String,
)

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
    val description: String? = null,
    val pending: Boolean = false,
    val canModify: Boolean = false,
) {
    val isRecurring: Boolean get() = recurringRuleId != null
    fun hasStarted(now: LocalDateTime): Boolean = !start.isAfter(now)
}

data class CancellationNotification(
    val id: Long,
    val courseName: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val roomName: String?,
)

data class EventRequest(
    val eventId: Long,
    val courseName: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val roomName: String?,
    val description: String?,
    val organizerName: String?,
)

data class EventResponseNotice(
    val id: Long,
    val accepted: Boolean,
    val courseName: String,
    val instructorName: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val roomName: String?,
)

data class EventNotifications(val requests: List<EventRequest>, val responses: List<EventResponseNotice>) {
    val count: Int get() = requests.size + responses.size
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
    val canModify: Boolean = false,
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
    val description: String? = null,
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

enum class RankingCategory(val apiKey: String, val label: String) {
    OVERALL("overall", "Absoluto"),
    ATTENDANCE("attendance", "Asistencia"),
    STRETCHING("stretching", "Estiramiento"),
    STRENGTH("strength", "Entrenamiento de fuerza"),
    CARDIO("cardio", "Cardio"),
    FORMS("forms", "Cantidad de formas"),
}

data class RankingEntry(val rank: Int, val studentId: Long, val name: String, val points: Int)

data class PerformanceRankings(val month: YearMonth, val categories: Map<RankingCategory, List<RankingEntry>>) {
    fun visibleEntries(category: RankingCategory, isAdmin: Boolean, expanded: Boolean): List<RankingEntry> {
        val entries = categories[category].orEmpty()
        return if (isAdmin && !expanded) entries.take(3) else entries
    }
}

enum class ConfirmationStatus(val label: String) {
    PENDING("pendiente"), CONFIRMED("confirmado"), REJECTED("rechazado"), NOT_REQUIRED("no requerido"),
}

enum class PerformanceDecision { CONFIRMED, REJECTED }

data class PerformanceForm(
    val id: Long,
    val name: String,
    val style: String,
    val known: Boolean = false,
    val inCurrentGrade: Boolean = false,
    val optional: Boolean = false,
)

data class PerformanceConfirmer(val id: Long, val name: String, val role: String)

data class PerformanceConfirmation(val studentId: Long, val name: String, val status: ConfirmationStatus)

data class PerformanceRequest(
    val id: Long,
    val studentName: String,
    val date: LocalDate,
    val physicalWork: Boolean,
    val cardioWork: Boolean,
    val stretching: Boolean,
    val attendedClass: Boolean,
    val forms: List<String>,
)

data class PerformanceRecord(
    val id: Long,
    val studentId: Long,
    val studentName: String,
    val date: LocalDate,
    val approvedAt: String?,
    val rejectedAt: String?,
    val resolvedByAdmin: Boolean,
    val physicalWork: Boolean,
    val cardioWork: Boolean,
    val stretching: Boolean,
    val attendedClass: Boolean,
    val forms: List<PerformanceForm>,
    val confirmations: List<PerformanceConfirmation>,
) {
    val statusLabel: String
        get() {
            if (resolvedByAdmin && rejectedAt != null) return "Rechazado por el administrador"
            if (resolvedByAdmin && approvedAt != null) return "Aprobado por el administrador"
            if (rejectedAt != null) return "Rechazado"
            if (approvedAt != null) return "Aprobado · 2 confirmaciones"
            val confirmed = confirmations.count { it.status == ConfirmationStatus.CONFIRMED }
            val prefix = if (confirmations.any { it.status == ConfirmationStatus.PENDING }) "Pendiente" else "Sin aprobar"
            return "$prefix · $confirmed/2 confirmaciones"
        }
}

data class PerformancePayload(
    val date: LocalDate,
    val physicalWork: Boolean,
    val cardioWork: Boolean,
    val stretching: Boolean,
    val attendedClass: Boolean,
    val formIds: List<Long>,
    val confirmerIds: List<Long>,
)
