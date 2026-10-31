package com.example.kungfuapplication.data.remote

import com.example.kungfuapplication.data.model.Attendees
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.CancellationNotification
import com.example.kungfuapplication.data.model.EventNotifications
import com.example.kungfuapplication.data.model.EventRequest
import com.example.kungfuapplication.data.model.EventResponseNotice
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
import com.example.kungfuapplication.data.model.ConfirmationStatus
import com.example.kungfuapplication.data.model.PerformanceConfirmation
import com.example.kungfuapplication.data.model.PerformanceConfirmer
import com.example.kungfuapplication.data.model.PerformanceForm
import com.example.kungfuapplication.data.model.PerformancePayload
import com.example.kungfuapplication.data.model.PerformanceRankings
import com.example.kungfuapplication.data.model.PerformanceRecord
import com.example.kungfuapplication.data.model.PerformanceRequest
import com.example.kungfuapplication.data.model.PromotionNotification
import com.example.kungfuapplication.data.model.RankingCategory
import com.example.kungfuapplication.data.model.RankingEntry
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
import java.time.YearMonth
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
        description = json.str("descripcion"),
        pending = json.str("estado_instructor") == "PENDIENTE",
        canModify = json.bool("puede_modificar") ?: false,
    )
}

fun parseCancellationNotification(json: JsonObject) = CancellationNotification(
    id = json.long("id_aviso") ?: error("Aviso de cancelación sin identificador"),
    courseName = json.str("curso_nombre").orEmpty(),
    start = json.dateTime("fecha_hora") ?: error("Aviso de cancelación sin fecha"),
    end = json.dateTime("fecha_hora_fin") ?: error("Aviso de cancelación sin hora de fin"),
    roomName = json.str("sala_nombre"),
)

fun parseEventNotifications(json: JsonObject) = EventNotifications(
    requests = json.objects("requests").map {
        EventRequest(
            eventId = it.long("id_curso_agendado") ?: error("Solicitud sin actividad"),
            courseName = it.str("curso_nombre").orEmpty(),
            start = it.dateTime("fecha_hora") ?: error("Solicitud sin fecha"),
            end = it.dateTime("fecha_hora_fin") ?: error("Solicitud sin hora de fin"),
            roomName = it.str("sala_nombre"),
            description = it.str("descripcion"),
            organizerName = it.str("organizador_nombre"),
        )
    },
    responses = json.objects("responses").map {
        EventResponseNotice(
            id = it.long("id_aviso") ?: error("Respuesta sin identificador"),
            accepted = it.str("decision") == "ACEPTADO",
            courseName = it.str("curso_nombre").orEmpty(),
            instructorName = it.str("instructor_nombre").orEmpty(),
            start = it.dateTime("fecha_hora") ?: error("Respuesta sin fecha"),
            end = it.dateTime("fecha_hora_fin") ?: error("Respuesta sin hora de fin"),
            roomName = it.str("sala_nombre"),
        )
    },
)

fun parsePromotionNotification(json: JsonObject) = PromotionNotification(
    id = json.long("id_aviso") ?: error("Aviso de ascenso sin identificador"),
    previousGrade = json.str("grado_anterior").orEmpty(),
    newGrade = json.str("grado_nuevo").orEmpty(),
)

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
    canModify = json.bool("puede_modificar") ?: false,
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
    if (description == null) put("description", JsonNull) else put("description", description)
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

fun parsePerformanceForm(json: JsonObject) = PerformanceForm(
    id = json.long("id") ?: error("Forma sin identificador"),
    name = json.str("name").orEmpty(),
    style = json.str("style").orEmpty(),
    known = json.bool("known") ?: false,
    inCurrentGrade = json.bool("inCurrentGrade") ?: false,
    optional = json.bool("optional") ?: false,
)

fun parsePerformanceConfirmer(json: JsonObject) = PerformanceConfirmer(
    id = json.long("id") ?: error("Confirmante sin identificador"),
    name = json.str("name").orEmpty(),
    role = json.str("role").orEmpty(),
)

fun parsePerformanceRequest(json: JsonObject) = PerformanceRequest(
    id = json.long("id") ?: error("Solicitud sin identificador"),
    studentName = json.str("studentName").orEmpty(),
    date = json.date("date") ?: error("Solicitud sin fecha"),
    physicalWork = json.bool("physicalWork") ?: false,
    cardioWork = json.bool("cardioWork") ?: false,
    stretching = json.bool("stretching") ?: false,
    attendedClass = json.bool("attendedClass") ?: false,
    forms = json.array("forms").mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
)

fun parsePerformanceRecord(json: JsonObject) = PerformanceRecord(
    id = json.long("id") ?: error("Desempeño sin identificador"),
    studentId = json.long("studentId") ?: error("Desempeño sin alumno"),
    studentName = json.str("studentName").orEmpty(),
    date = json.date("date") ?: error("Desempeño sin fecha"),
    approvedAt = json.str("approvedAt"),
    rejectedAt = json.str("rejectedAt"),
    resolvedByAdmin = json.bool("resolvedByAdmin") ?: false,
    physicalWork = json.bool("physicalWork") ?: false,
    cardioWork = json.bool("cardioWork") ?: false,
    stretching = json.bool("stretching") ?: false,
    attendedClass = json.bool("attendedClass") ?: false,
    forms = json.objects("forms").map(::parsePerformanceForm),
    confirmations = json.objects("confirmations").map {
        PerformanceConfirmation(
            studentId = it.long("studentId") ?: error("Confirmación sin alumno"),
            name = it.str("name").orEmpty(),
            status = ConfirmationStatus.valueOf(it.str("status") ?: "PENDING"),
        )
    },
)

fun parsePerformanceRankings(json: JsonObject) = PerformanceRankings(
    month = YearMonth.parse(json.str("month")),
    categories = RankingCategory.entries.associateWith { category ->
        json.objects(category.apiKey).map {
            RankingEntry(
                rank = it.int("rank") ?: 0,
                studentId = it.long("studentId") ?: error("Ranking sin alumno"),
                name = it.str("name").orEmpty(),
                points = it.int("points") ?: 0,
            )
        }
    },
)

fun PerformancePayload.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("physicalWork", physicalWork)
    put("cardioWork", cardioWork)
    put("stretching", stretching)
    put("attendedClass", attendedClass)
    putJsonArray("formIds") { formIds.distinct().forEach { add(JsonPrimitive(it)) } }
    putJsonArray("confirmerIds") { confirmerIds.distinct().forEach { add(JsonPrimitive(it)) } }
}
