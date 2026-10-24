package com.example.kungfuapplication.data.remote

import com.example.kungfuapplication.data.model.Attendees
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.Course
import com.example.kungfuapplication.data.model.CourseType
import com.example.kungfuapplication.data.model.EventPayload
import com.example.kungfuapplication.data.model.FormProgress
import com.example.kungfuapplication.data.model.GradeFormItem
import com.example.kungfuapplication.data.model.GradeInfo
import com.example.kungfuapplication.data.model.InstructorInfo
import com.example.kungfuapplication.data.model.InstructorOption
import com.example.kungfuapplication.data.model.JoinResult
import com.example.kungfuapplication.data.model.KnownForm
import com.example.kungfuapplication.data.model.MyParticipation
import com.example.kungfuapplication.data.model.MyProfile
import com.example.kungfuapplication.data.model.Participant
import com.example.kungfuapplication.data.model.ParticipationRole
import com.example.kungfuapplication.data.model.ParticipationSettings
import com.example.kungfuapplication.data.model.RecurringRule
import com.example.kungfuapplication.data.model.Room
import com.example.kungfuapplication.data.model.RulePayload
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.StudentInfo
import com.example.kungfuapplication.data.model.UserRole
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val apiDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val apiTime = DateTimeFormatter.ofPattern("HH:mm")

/** Formato que esperan los parámetros `from`/`to` y los cuerpos de la agenda (hora local de Madrid). */
fun LocalDateTime.toApiDateTime(): String = format(apiDateTime)
fun LocalTime.toApiTime(): String = format(apiTime)

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()
private fun JsonObject.objects(key: String): List<JsonObject> = array(key).mapNotNull { it as? JsonObject }

fun parseSessionUser(json: JsonObject): SessionUser = SessionUser(
    role = runCatching { UserRole.valueOf(json.str("role").orEmpty().uppercase()) }.getOrDefault(UserRole.USER),
    userId = json.long("userId"),
    displayName = json.str("displayName") ?: json.str("email") ?: "Usuario",
    studentId = json.long("studentId"),
    instructorId = json.long("instructorId"),
)

fun parseProfile(json: JsonObject): MyProfile {
    val student = json.obj("student") ?: error("Respuesta sin datos del alumno")
    return MyProfile(
        student = StudentInfo(
            id = student.long("id") ?: 0,
            name = student.str("name").orEmpty(),
            enrollmentNumber = student.str("enrollmentNumber"),
            email = student.str("email"),
            birthDate = student.date("birthDate"),
            joinedOn = student.date("joinedOn"),
            active = student.bool("active") ?: true,
            isInstructor = student.bool("isInstructor") ?: false,
        ),
        grade = json.obj("grade")?.let {
            GradeInfo(
                id = it.long("id") ?: 0,
                name = it.str("name").orEmpty(),
                beltColor = it.str("beltColor"),
                order = it.int("order"),
                requiredForms = it.int("requiredForms"),
            )
        },
        knownForms = json.objects("knownForms").map {
            KnownForm(
                id = it.long("id") ?: 0,
                name = it.str("name").orEmpty(),
                description = it.str("description"),
                style = it.str("style"),
                learnedOn = it.date("learnedOn"),
            )
        },
        gradeForms = json.objects("gradeForms").map {
            GradeFormItem(
                id = it.long("id") ?: 0,
                name = it.str("name").orEmpty(),
                description = it.str("description"),
                style = it.str("style"),
                optional = it.bool("optional") ?: false,
                known = it.bool("known") ?: false,
                learning = it.obj("learning")?.let { progress ->
                    FormProgress(percent = progress.int("percent") ?: 0, updatedOn = progress.date("updatedOn"))
                },
            )
        },
        instructor = json.obj("instructor")?.let {
            InstructorInfo(
                id = it.long("id") ?: 0,
                specialty = it.str("specialty"),
                hiredOn = it.date("hiredOn"),
                active = it.bool("active") ?: true,
                styles = it.array("styles").mapNotNull { style -> (style as? JsonPrimitive)?.contentOrNull },
            )
        },
    )
}

private fun courseType(value: String?): CourseType =
    if (value == "CURSO_ESPECIAL") CourseType.CURSO_ESPECIAL else CourseType.HABITUAL

private fun participationRole(value: String?): ParticipationRole =
    if (value == "INSTRUCTOR") ParticipationRole.INSTRUCTOR else ParticipationRole.ALUMNO

fun parseEvent(json: JsonObject): CalendarEvent {
    val start = json.dateTime("fecha_hora") ?: error("Actividad sin fecha")
    return CalendarEvent(
        id = json.long("id_curso_agendado") ?: error("Actividad sin identificador"),
        courseId = json.long("id_curso") ?: 0,
        instructorId = json.long("id_instructor"),
        roomId = json.long("id_sala"),
        start = start,
        end = json.dateTime("fecha_hora_fin") ?: start.plusHours(1),
        active = json.bool("activo") ?: true,
        recurringRuleId = json.long("id_horario_recurrente"),
        modifiedManually = json.bool("modificado_manualmente") ?: false,
        courseName = json.str("curso_nombre") ?: "Actividad",
        courseTopic = json.str("curso_tema"),
        courseDescription = json.str("curso_descripcion"),
        courseType = courseType(json.str("tipo_curso")),
        instructorName = json.str("instructor_nombre"),
        roomName = json.str("sala_nombre"),
        participantLimit = json.int("limite_participantes"),
        showAttendeeCount = json.bool("mostrar_asistentes") ?: true,
        attendees = json.obj("asistentes")?.let {
            Attendees(students = it.int("alumnos") ?: 0, instructors = it.int("instructores") ?: 0)
        },
        full = json.bool("completo") ?: false,
        myParticipation = json.obj("mi_participacion")?.let {
            MyParticipation(role = participationRole(it.str("rol")), series = it.bool("serie") ?: false)
        },
    )
}

fun parseRoom(json: JsonObject) = Room(
    id = json.long("id_sala") ?: 0,
    name = json.str("nombre").orEmpty(),
    active = json.bool("activo") ?: true,
)

fun parseCourse(json: JsonObject) = Course(
    id = json.long("id_curso") ?: 0,
    name = json.str("nombre").orEmpty(),
    topic = json.str("tema"),
    description = json.str("descripcion"),
    type = courseType(json.str("tipo_curso")),
)

fun parseInstructor(json: JsonObject) = InstructorOption(
    id = json.long("id_instructor") ?: 0,
    name = json.str("nombre").orEmpty(),
    active = json.bool("activo") ?: true,
)

private fun parseTime(value: String?): LocalTime? = value?.let { runCatching { LocalTime.parse(it.take(8)) }.getOrNull() }

fun parseRule(json: JsonObject) = RecurringRule(
    id = json.long("id_horario_recurrente") ?: 0,
    courseId = json.long("id_curso") ?: 0,
    courseName = json.str("curso_nombre").orEmpty(),
    courseType = courseType(json.str("tipo_curso")),
    instructorId = json.long("id_instructor"),
    instructorName = json.str("instructor_nombre"),
    roomId = json.long("id_sala"),
    roomName = json.str("sala_nombre"),
    weekday = json.int("dia_semana") ?: 1,
    startTime = parseTime(json.str("hora_inicio")) ?: LocalTime.of(0, 0),
    endTime = parseTime(json.str("hora_fin")) ?: LocalTime.of(0, 0),
    startsOn = json.date("fecha_inicio") ?: LocalDate.now(),
    endsOn = json.date("fecha_fin"),
    active = json.bool("activo") ?: true,
    participantLimit = json.int("limite_participantes"),
    showAttendeeCount = json.bool("mostrar_asistentes") ?: true,
)

fun parseParticipant(json: JsonObject) = Participant(
    studentId = json.long("id_alumno") ?: 0,
    name = json.str("nombre").orEmpty(),
    role = participationRole(json.str("rol")),
    series = json.bool("serie") ?: false,
)

fun parseJoinResult(json: JsonObject) = JoinResult(
    joined = json.array("joined").size,
    skipped = json.array("skipped").mapNotNull { parseBackendDate((it as? JsonPrimitive)?.contentOrNull) },
)

fun List<JsonElement>.objectsOnly(): List<JsonObject> = mapNotNull { it as? JsonObject }

private fun JsonObjectBuilder.putSettings(settings: ParticipationSettings?) {
    if (settings == null) return
    if (settings.participantLimit == null) put("participantLimit", JsonNull) else put("participantLimit", settings.participantLimit)
    put("showAttendeeCount", settings.showAttendeeCount)
}

fun EventPayload.toJson(): JsonObject = buildJsonObject {
    put("courseId", courseId)
    put("instructorId", instructorId)
    put("roomId", roomId)
    put("startAt", startAt.toApiDateTime())
    put("endAt", endAt.toApiDateTime())
    putSettings(settings)
}

fun RulePayload.toJson(): JsonObject = buildJsonObject {
    put("courseId", courseId)
    put("instructorId", instructorId)
    put("roomId", roomId)
    putJsonArray("weekdays") { weekdays.forEach { add(JsonPrimitive(it)) } }
    put("startTime", startTime.toApiTime())
    put("endTime", endTime.toApiTime())
    put("startsOn", startsOn.toString())
    if (endsOn == null) put("endsOn", JsonNull) else put("endsOn", endsOn.toString())
    putSettings(settings)
}

internal fun JsonElement.asObject(): JsonObject = jsonObject
