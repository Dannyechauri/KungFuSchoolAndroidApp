package com.example.kungfuapplication.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))

fun plural(count: Int, singular: String, pluralForm: String): String =
    "$count ${if (count == 1) singular else pluralForm}"

/** Mismo texto que el panel de participantes de la web. */
fun CalendarEvent.attendanceSummary(): String {
    val counts = attendees
    if (counts == null) {
        if (full) return "Plazas completas"
        return if (participantLimit == null) "Sin límite de plazas" else "Quedan plazas libres"
    }
    val students = if (participantLimit == null) {
        plural(counts.students, "alumno apuntado", "alumnos apuntados")
    } else {
        "${counts.students} / $participantLimit plazas ocupadas"
    }
    val instructors = plural(counts.instructors, "instructor", "instructores")
    val limit = if (participantLimit == null) " · Sin límite de plazas" else ""
    return "$students · $instructors$limit"
}

/** Texto breve para las tarjetas de la lista. */
fun CalendarEvent.shortAttendance(): String? = when {
    !active -> null
    full -> "Completo"
    attendees != null && participantLimit != null -> "${attendees.students}/$participantLimit"
    attendees != null -> attendees.students.toString()
    else -> null
}

fun CalendarEvent.myParticipationText(): String? {
    val mine = myParticipation ?: return null
    val scope = if (isRecurring) (if (mine.series) " a toda la serie" else " solo a este día") else ""
    return "Estás apuntado como ${mine.role.label}$scope."
}

/** Para alumnos, una sesión completa bloquea apuntarse solo a ese día. */
fun CalendarEvent.isSeatBlocked(role: ParticipationRole, scope: ParticipationScope): Boolean =
    role == ParticipationRole.ALUMNO && scope == ParticipationScope.OCCURRENCE && full

fun joinMessage(result: JoinResult, role: ParticipationRole, scope: ParticipationScope, recurring: Boolean): String {
    if (scope == ParticipationScope.SERIES && recurring) {
        val skipped = result.skipped.joinToString(", ") { it.format(shortDate).trimEnd('.') }
        val suffix = if (result.skipped.isNotEmpty()) " Sin plaza: $skipped." else ""
        return "Te has apuntado a ${plural(result.joined, "fecha", "fechas")} de la serie.$suffix"
    }
    return "Te has apuntado como ${role.label}."
}

fun leaveMessage(scope: ParticipationScope): String =
    if (scope == ParticipationScope.SERIES) "Te has quitado de la serie desde este día." else "Te has quitado de este día."

/** Prefijo que usa la web cuando el backend detecta una sala ocupada. */
fun calendarErrorText(status: Int, message: String): String =
    if (status == 409 && message.lowercase().contains("solapa")) "La sala ya está ocupada en ese horario. $message" else message

val weekdayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
val weekdayShortNames = listOf("L", "M", "X", "J", "V", "S", "D")

fun weekdayName(day: Int): String = weekdayNames.getOrElse(day - 1) { "Día $day" }

fun LocalDate.isoWeekday(): Int = dayOfWeek.value
