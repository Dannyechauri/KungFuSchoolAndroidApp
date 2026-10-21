# 🥋 Kung Fu School Management App - Specification

## 📱 Overview
This project is a mobile (Android) and web application to manage a Kung Fu school.

It includes:
- Student progress tracking
- Course scheduling
- Challenges (retos)
- Martial forms tracking
- Belt system (levels)
- Exams and syllabus

---

## 👥 User Roles

### Student
- View courses and calendar
- Enroll in courses
- Track progress (stretching, forms)
- View exams and requirements

### Instructor
- Create and manage courses
- Create challenges
- View student progress
- Add observations to exams

### Master
- Full system control
- Define exam syllabus
- Assign belt levels
- View all student data

---

## 🧩 Core Features

### 1. Stretch Tracking
Categories:
- Front
- Side
- Shoulders
- Back

Students can:
- Mark when they complete stretches
- Track history and progress

---

### 2. Challenges (Retos)
- Created by instructors or master
- Students can join and track progress

---

### 3. Courses & Calendar

#### Features:
- Calendar views:
    - Daily
    - Weekly
    - Monthly
    - Yearly

#### Permissions:
- Instructors / Master:
    - Create, edit, delete courses
- Students:
    - View calendar
    - Enroll in courses

---

### 4. Martial Forms

Forms are grouped by:
- Style
- Empty hand
- Weapons

Students can:
- Mark learned forms
- Build a "martial CV"

Instructors/Master can:
- View all student progress

---

### 5. Belt / Level System

- Students have a belt level
- Each belt unlocks specific forms
- Forms have a minimum required level

Rules:
- Students cannot mark forms above their level
- Progress depends on belt

---

### 6. Exams System

#### Syllabus (defined ONLY by Master):
- Forms
- Sansao
- Combat (only for age 18–30)
- Applications of forms

#### Permissions:
- Master:
    - Define and assign syllabus
- Instructor:
    - Add observations
- System:
    - Track results
    - Update belt level

---

## 📐 UI / UX (Android)

### Navigation (Bottom Bar)
- Home
- Courses
- Progress
- Challenges
- Profile

---

### Home Screen
- User info (name + belt)
- Weekly progress
- Next class
- Active challenge
- Notifications

---

### Courses Screen
- Calendar view
- Enrollment button
- Instructor mode:
    - Create/edit courses
    - Drag & drop scheduling

---

### Progress Screen (Tabs)
- Forms
- Stretching
- Belt / Exam status

---

### Challenges Screen
- Active challenges
- Available challenges
- Join button

---

### Profile Screen
- User info
- Stats
- Edit profile

---

## 🧱 Tech Stack (Recommended)

- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM
- State: StateFlow
- Backend: REST API
- Database: PostgreSQL

---

## 🧠 Dev Guidelines (for Copilot)

- Use Kotlin + Jetpack Compose
- Follow MVVM architecture
- Use clean architecture principles where possible
- Prefer StateFlow over LiveData
- Use modular structure (feature-based)

---

## 🎯 Future Enhancements

- Notifications (reminders)
- Leaderboards / gamification
- Payments integration
- Advanced analytics

---

## ✅ Estado actual de la app Android (integrada con KungFuBackendService)

La app ya no usa datos ficticios: consume la API REST de `KungFuBackendService` con sesión por cookie (`JSESSIONID`).

### Configurar el servidor
- URL por defecto: `http://10.0.2.2:8080` (el `localhost` del PC visto desde el emulador).
- En un móvil físico, abrir **Servidor** en la pantalla de login e indicar la IP del PC (p. ej. `http://192.168.1.50:8080`).
- Se permite tráfico HTTP (`usesCleartextTraffic`) para desarrollo; en producción usar HTTPS.

### Navegación implementada
| Pestaña | Datos del backend | Acciones |
|---|---|---|
| Inicio | perfil, grado, próximas clases, asistencias | abrir clase |
| Clases | `cursos_agendados`, `cursos`, `instructores`, `asistencias_curso`, `inscripciones` | confirmar asistencia (una clase o toda la serie futura), cancelar |
| Progreso / Temario | `grados`, `formas`, `estilos`, `grado_forma`, `alumno_forma` | solo lectura |
| Perfil | `alumnos` / `instructores`, `instructor_estilo` | cerrar sesión |

### Endpoints usados
- `POST /api/auth/login`, `GET /api/auth/me`, `DELETE /api/auth/logout`
- `GET /api/database/tables/{tabla}/rows?limit=500`
- `POST` / `DELETE /api/database/tables/asistencias_curso/rows` (única tabla que un usuario no administrador puede modificar)

### Pendiente (no existe aún en el backend)
- Retos y estiramientos (no hay tablas ni endpoints).
- Exámenes / temario de examen.
- Marcar formas aprendidas desde la app (el backend solo permite escribir `alumno_forma` a administradores).
- Crear/editar cursos desde la app como instructor.