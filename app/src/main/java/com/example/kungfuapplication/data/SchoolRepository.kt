package com.example.kungfuapplication.data

import com.example.kungfuapplication.data.model.Attendance
import com.example.kungfuapplication.data.model.Course
import com.example.kungfuapplication.data.model.Enrollment
import com.example.kungfuapplication.data.model.Grade
import com.example.kungfuapplication.data.model.GradeForm
import com.example.kungfuapplication.data.model.Instructor
import com.example.kungfuapplication.data.model.InstructorStyle
import com.example.kungfuapplication.data.model.LearnedForm
import com.example.kungfuapplication.data.model.MartialForm
import com.example.kungfuapplication.data.model.PersonType
import com.example.kungfuapplication.data.model.ScheduledClass
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.Student
import com.example.kungfuapplication.data.model.Style
import com.example.kungfuapplication.data.model.UserProfile
import com.example.kungfuapplication.data.remote.ApiClient
import com.example.kungfuapplication.data.remote.ApiException
import com.example.kungfuapplication.data.remote.bool
import com.example.kungfuapplication.data.remote.date
import com.example.kungfuapplication.data.remote.dateTime
import com.example.kungfuapplication.data.remote.int
import com.example.kungfuapplication.data.remote.long
import com.example.kungfuapplication.data.remote.str
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

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
        val user = api.post("/api/auth/login", body).jsonObject.toSessionUser()
        settings.lastEmail = email.trim()
        return user
    }

    /** Devuelve el usuario de la sesión guardada, o null si no hay sesión activa en el servidor. */
    suspend fun currentSession(): SessionUser? = try {
        api.get("/api/auth/me").jsonObject.toSessionUser()
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

    // ---------- Datos ----------

    suspend fun loadSchoolData(session: SessionUser): SchoolData = coroutineScope {
        val students = async { api.tableRows("alumnos").map { it.toStudent() } }
        val instructors = async { api.tableRows("instructores").map { it.toInstructor() } }
        val grades = async { api.tableRows("grados").map { it.toGrade() }.sortedBy { it.order } }
        val styles = async { api.tableRows("estilos").map { it.toStyle() }.sortedBy { it.name } }
        val forms = async { api.tableRows("formas").map { it.toForm() } }
        val gradeForms = async { optional { api.tableRows("grado_forma").map { it.toGradeForm() } } }
        val learned = async { optional { api.tableRows("alumno_forma").map { it.toLearnedForm() } } }
        val courses = async { api.tableRows("cursos").map { it.toCourse() } }
        val classes = async { api.tableRows("cursos_agendados").map { it.toScheduledClass() } }
        val attendances = async { optional { loadAttendances() } }
        val enrollments = async { optional { api.tableRows("inscripciones").map { it.toEnrollment() } } }
        val instructorStyles = async { optional { api.tableRows("instructor_estilo").map { it.toInstructorStyle() } } }

        val studentList = students.await()
        val instructorList = instructors.await()

        SchoolData(
            profile = resolveProfile(session, studentList, instructorList),
            grades = grades.await(),
            styles = styles.await(),
            forms = forms.await(),
            gradeForms = gradeForms.await(),
            learnedForms = learned.await(),
            courses = courses.await(),
            classes = classes.await(),
            instructors = instructorList,
            students = studentList,
            attendances = attendances.await(),
            enrollments = enrollments.await(),
            instructorStyles = instructorStyles.await(),
        )
    }

    suspend fun loadAttendances(): List<Attendance> =
        api.tableRows("asistencias_curso").map { it.toAttendance() }

    /** Tablas secundarias: si fallan (salvo sesión caducada) la app sigue funcionando sin ellas. */
    private suspend fun <T> optional(block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: ApiException) {
        if (e.isUnauthorized) throw e else emptyList()
    }

    suspend fun confirmAttendance(profile: UserProfile, scheduledClassId: Long) {
        val payload = buildJsonObject {
            put("id_curso_agendado", scheduledClassId)
            PersonType.entries.forEach { type ->
                if (type == profile.personType) put(type.attendanceColumn, profile.session.userId)
                else put(type.attendanceColumn, JsonNull)
            }
        }
        api.insertRow("asistencias_curso", payload)
    }

    suspend fun cancelAttendance(attendanceId: Long) {
        api.deleteRow("asistencias_curso", attendanceId.toString())
    }

    /**
     * El backend devuelve el rol USER tanto para alumnos como para instructores,
     * así que se identifica la persona por id + nombre mostrado (como la web, pero evitando
     * confundir un alumno y un instructor que compartan id).
     */
    private fun resolveProfile(
        session: SessionUser,
        students: List<Student>,
        instructors: List<Instructor>,
    ): UserProfile {
        if (session.isAdmin) {
            return UserProfile(session, PersonType.ADMIN)
        }
        val name = session.displayName.trim().lowercase()
        val student = students.firstOrNull { it.id == session.userId && it.name.trim().lowercase() == name }
        val instructor = instructors.firstOrNull { it.id == session.userId && it.name.trim().lowercase() == name }

        return when {
            student != null -> UserProfile(
                session,
                PersonType.STUDENT,
                student = student,
                instructor = instructors.firstOrNull {
                    !student.email.isNullOrBlank() && it.email.equals(student.email, ignoreCase = true)
                },
            )
            instructor != null -> UserProfile(session, PersonType.INSTRUCTOR, instructor = instructor)
            else -> {
                val byIdStudent = students.firstOrNull { it.id == session.userId }
                if (byIdStudent != null) {
                    UserProfile(session, PersonType.STUDENT, student = byIdStudent)
                } else {
                    UserProfile(
                        session,
                        PersonType.INSTRUCTOR,
                        instructor = instructors.firstOrNull { it.id == session.userId },
                    )
                }
            }
        }
    }
}

// ---------- Mapeo de filas ----------

private fun JsonObject.toSessionUser() = SessionUser(
    role = str("role") ?: "USER",
    userId = long("userId") ?: 0,
    displayName = str("displayName") ?: "Usuario",
)

private fun JsonObject.toStudent() = Student(
    id = long("id_alumno") ?: 0,
    name = str("nombre") ?: str("numero_matricula") ?: "Alumno",
    enrollmentNumber = str("numero_matricula"),
    joinDate = date("fecha_ingreso"),
    active = bool("activo") ?: true,
    birthDate = date("fecha_nacimiento"),
    gradeId = long("id_grado"),
    email = str("correo_electronico"),
)

private fun JsonObject.toInstructor() = Instructor(
    id = long("id_instructor") ?: 0,
    name = str("nombre") ?: "Instructor",
    specialty = str("especialidad"),
    email = str("correo_electronico"),
    active = bool("activo") ?: true,
    hireDate = date("fecha_contratacion"),
)

private fun JsonObject.toGrade() = Grade(
    id = long("id_grado") ?: 0,
    name = str("nombre") ?: "Grado",
    order = int("orden_grado") ?: 0,
    beltColor = str("color_cinturon") ?: "",
    requiredForms = int("formas_requeridas") ?: 0,
)

private fun JsonObject.toStyle() = Style(
    id = long("id_estilo") ?: 0,
    name = str("nombre") ?: "Estilo",
    description = str("descripcion"),
)

private fun JsonObject.toForm() = MartialForm(
    id = long("id_forma") ?: 0,
    styleId = long("id_estilo") ?: 0,
    name = str("nombre") ?: "Forma",
    description = str("descripcion"),
)

private fun JsonObject.toGradeForm() = GradeForm(
    gradeId = long("id_grado") ?: 0,
    formId = long("id_forma") ?: 0,
    optional = bool("es_opcional") ?: false,
)

private fun JsonObject.toLearnedForm() = LearnedForm(
    studentId = long("id_alumno") ?: 0,
    formId = long("id_forma") ?: 0,
    learnedOn = date("fecha_aprendida"),
)

private fun JsonObject.toCourse() = Course(
    id = long("id_curso") ?: 0,
    name = str("nombre") ?: "Curso",
    topic = str("tema"),
    description = str("descripcion"),
)

private fun JsonObject.toScheduledClass() = ScheduledClass(
    id = long("id_curso_agendado") ?: 0,
    courseId = long("id_curso") ?: 0,
    instructorId = long("id_instructor"),
    dateTime = dateTime("fecha_hora"),
    active = bool("activo") ?: true,
)

private fun JsonObject.toAttendance() = Attendance(
    id = long("id_asistencia") ?: 0,
    scheduledClassId = long("id_curso_agendado") ?: 0,
    studentId = long("id_alumno"),
    instructorId = long("id_instructor"),
    adminId = long("id_administrador"),
    registeredAt = dateTime("fecha_registro"),
)

private fun JsonObject.toEnrollment() = Enrollment(
    id = long("id_inscripcion") ?: 0,
    studentId = long("id_alumno") ?: 0,
    scheduledClassId = long("id_curso_agendado") ?: 0,
    date = date("fecha_inscripcion"),
    status = str("estado") ?: "activa",
)

private fun JsonObject.toInstructorStyle() = InstructorStyle(
    instructorId = long("id_instructor") ?: 0,
    styleId = long("id_estilo") ?: 0,
)
