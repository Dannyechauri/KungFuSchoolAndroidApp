package com.example.kungfuapplication.data.model

import java.time.LocalDateTime

/** Instantánea de todos los datos que la app obtiene del backend para el usuario actual. */
data class SchoolData(
    val profile: UserProfile,
    val grades: List<Grade>,
    val styles: List<Style>,
    val forms: List<MartialForm>,
    val gradeForms: List<GradeForm>,
    val learnedForms: List<LearnedForm>,
    val courses: List<Course>,
    val classes: List<ScheduledClass>,
    val instructors: List<Instructor>,
    val students: List<Student>,
    val attendances: List<Attendance>,
    val enrollments: List<Enrollment>,
    val instructorStyles: List<InstructorStyle>,
) {
    private val gradesById = grades.associateBy { it.id }
    private val coursesById = courses.associateBy { it.id }
    private val instructorsById = instructors.associateBy { it.id }
    private val studentsById = students.associateBy { it.id }
    val stylesById: Map<Long, Style> = styles.associateBy { it.id }

    val currentGrade: Grade? = profile.student?.gradeId?.let { gradesById[it] }

    val nextGrade: Grade? = currentGrade?.let { current ->
        grades.filter { it.order > current.order }.minByOrNull { it.order }
    }

    val myLearnedForms: Map<Long, LearnedForm> =
        profile.student?.let { student ->
            learnedForms.filter { it.studentId == student.id }.associateBy { it.formId }
        } ?: emptyMap()

    /** Grado mínimo en el que aparece cada forma (según `grado_forma`). */
    private val minimumGradeByForm: Map<Long, Grade> = gradeForms
        .groupBy { it.formId }
        .mapNotNull { (formId, links) ->
            links.mapNotNull { gradesById[it.gradeId] }.minByOrNull { it.order }?.let { formId to it }
        }
        .toMap()

    fun minimumGradeFor(formId: Long): Grade? = minimumGradeByForm[formId]

    fun formStatus(form: MartialForm): FormStatus {
        myLearnedForms[form.id]?.let { return FormStatus.Learned(it.learnedOn) }
        val required = minimumGradeByForm[form.id] ?: return FormStatus.Available
        val current = currentGrade ?: return FormStatus.Locked(required)
        return if (required.order <= current.order) FormStatus.Available else FormStatus.Locked(required)
    }

    fun formsForGrade(gradeId: Long): List<Pair<MartialForm, GradeForm>> {
        val formsById = forms.associateBy { it.id }
        return gradeForms.filter { it.gradeId == gradeId }
            .mapNotNull { link -> formsById[link.formId]?.let { it to link } }
            .sortedBy { it.first.name }
    }

    /** Progreso hacia el siguiente cinturón según las formas obligatorias de ese grado. */
    val nextGradeProgress: GradeProgress? = nextGrade?.let { next ->
        val mandatory = formsForGrade(next.id).filter { !it.second.optional }.map { it.first }
        val learned = mandatory.count { it.id in myLearnedForms }
        val total = maxOf(next.requiredForms, mandatory.size)
        GradeProgress(next, learned = minOf(learned, total), total = total)
    }

    fun course(id: Long): Course? = coursesById[id]
    fun instructor(id: Long?): Instructor? = id?.let { instructorsById[it] }
    fun grade(id: Long?): Grade? = id?.let { gradesById[it] }

    fun isMyAttendance(attendance: Attendance): Boolean {
        val id = profile.session.userId
        return when (profile.personType) {
            PersonType.STUDENT -> attendance.studentId == id
            PersonType.INSTRUCTOR -> attendance.instructorId == id
            PersonType.ADMIN -> attendance.adminId == id
        }
    }

    private val attendancesByClass = attendances.groupBy { it.scheduledClassId }

    val classItems: List<ClassItem> = classes
        .map { scheduled ->
            val classAttendances = attendancesByClass[scheduled.id].orEmpty()
            ClassItem(
                scheduled = scheduled,
                course = coursesById[scheduled.courseId],
                instructor = scheduled.instructorId?.let { instructorsById[it] },
                attendees = classAttendances,
                myAttendance = classAttendances.firstOrNull { isMyAttendance(it) },
                enrolled = profile.student != null && enrollments.any {
                    it.studentId == profile.student.id &&
                        it.scheduledClassId == scheduled.id &&
                        it.status.equals("activa", ignoreCase = true)
                },
                teaching = profile.instructor != null && scheduled.instructorId == profile.instructor.id,
            )
        }
        .sortedWith(compareBy(nullsLast()) { it.scheduled.dateTime })

    fun classItem(id: Long): ClassItem? = classItems.firstOrNull { it.scheduled.id == id }

    fun upcomingClasses(now: LocalDateTime): List<ClassItem> =
        classItems.filter { it.scheduled.active && it.scheduled.dateTime?.isBefore(now) == false }

    fun myUpcomingClasses(now: LocalDateTime): List<ClassItem> = upcomingClasses(now).filter { it.isMine }

    fun attendedClassesCount(now: LocalDateTime): Int =
        classItems.count { it.myAttendance != null && it.scheduled.dateTime?.isBefore(now) == true }

    /** Próximas ocurrencias de la misma actividad (mismo curso y misma hora), como hace la web. */
    fun seriesFrom(item: ClassItem, now: LocalDateTime): List<ClassItem> {
        val start = item.scheduled.dateTime ?: return listOf(item)
        return upcomingClasses(now)
            .filter {
                val dt = it.scheduled.dateTime!!
                it.scheduled.courseId == item.scheduled.courseId &&
                    dt.toLocalTime() == start.toLocalTime() &&
                    !dt.toLocalDate().isBefore(start.toLocalDate())
            }
            .distinctBy { it.scheduled.dateTime!!.toLocalDate() }
    }

    fun attendeeName(attendance: Attendance): String = when {
        attendance.studentId != null -> studentsById[attendance.studentId]?.name ?: "Alumno ${attendance.studentId}"
        attendance.instructorId != null -> instructorsById[attendance.instructorId]?.name ?: "Instructor ${attendance.instructorId}"
        attendance.adminId != null -> "Administrador ${attendance.adminId}"
        else -> "Desconocido"
    }

    fun stylesOfInstructor(instructorId: Long): List<Style> =
        instructorStyles.filter { it.instructorId == instructorId }.mapNotNull { stylesById[it.styleId] }
}

data class ClassItem(
    val scheduled: ScheduledClass,
    val course: Course?,
    val instructor: Instructor?,
    val attendees: List<Attendance>,
    val myAttendance: Attendance?,
    val enrolled: Boolean,
    val teaching: Boolean,
) {
    val title: String get() = course?.name ?: "Curso ${scheduled.courseId}"
    val isMine: Boolean get() = myAttendance != null || enrolled || teaching
}

data class GradeProgress(val nextGrade: Grade, val learned: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 1f else learned.toFloat() / total
}

sealed interface FormStatus {
    data class Learned(val date: java.time.LocalDate?) : FormStatus
    data object Available : FormStatus
    data class Locked(val requiredGrade: Grade) : FormStatus
}
