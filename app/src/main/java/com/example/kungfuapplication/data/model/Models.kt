package com.example.kungfuapplication.data.model

import java.time.LocalDate
import java.time.LocalDateTime

data class SessionUser(
    val role: String,
    val userId: Long,
    val displayName: String,
) {
    val isAdmin: Boolean get() = role == "ADMIN"
}

/** Tipo de persona con el que se registra la asistencia en `asistencias_curso`. */
enum class PersonType(val label: String, val attendanceColumn: String) {
    STUDENT("Alumno", "id_alumno"),
    INSTRUCTOR("Instructor", "id_instructor"),
    ADMIN("Administrador", "id_administrador"),
}

data class UserProfile(
    val session: SessionUser,
    val personType: PersonType,
    val student: Student? = null,
    val instructor: Instructor? = null,
) {
    val displayName: String get() = student?.name ?: instructor?.name ?: session.displayName
    val email: String? get() = student?.email ?: instructor?.email
}

data class Student(
    val id: Long,
    val name: String,
    val enrollmentNumber: String?,
    val joinDate: LocalDate?,
    val active: Boolean,
    val birthDate: LocalDate?,
    val gradeId: Long?,
    val email: String?,
)

data class Instructor(
    val id: Long,
    val name: String,
    val specialty: String?,
    val email: String?,
    val active: Boolean,
    val hireDate: LocalDate?,
)

data class Grade(
    val id: Long,
    val name: String,
    val order: Int,
    val beltColor: String,
    val requiredForms: Int,
)

data class Style(
    val id: Long,
    val name: String,
    val description: String?,
)

data class MartialForm(
    val id: Long,
    val styleId: Long,
    val name: String,
    val description: String?,
)

data class GradeForm(
    val gradeId: Long,
    val formId: Long,
    val optional: Boolean,
)

data class LearnedForm(
    val studentId: Long,
    val formId: Long,
    val learnedOn: LocalDate?,
)

data class Course(
    val id: Long,
    val name: String,
    val topic: String?,
    val description: String?,
)

data class ScheduledClass(
    val id: Long,
    val courseId: Long,
    val instructorId: Long?,
    val dateTime: LocalDateTime?,
    val active: Boolean,
)

data class Attendance(
    val id: Long,
    val scheduledClassId: Long,
    val studentId: Long?,
    val instructorId: Long?,
    val adminId: Long?,
    val registeredAt: LocalDateTime?,
)

data class Enrollment(
    val id: Long,
    val studentId: Long,
    val scheduledClassId: Long,
    val date: LocalDate?,
    val status: String,
)

data class InstructorStyle(
    val instructorId: Long,
    val styleId: Long,
)
