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

### 7. Daily Performance

- Students and linked instructors can register or replace a daily session, select activity checkboxes and up to 30 forms, and choose 2 to 100 witnesses.
- Requests can be confirmed or rejected. Two confirmations approve a session; pending requests close when the backend resolves it.
- Administrators can approve or reject any open request without waiting for witnesses. History displays "Aprobado por el administrador" or "Rechazado por el administrador" for these decisions.
- Monthly rankings include overall, attendance, stretching, strength, cardio, and forms. Administrators can expand each category independently; other accounts see the top three plus their own position.
- The performance navigation item is highlighted with a pending-request count for students and instructors. Requests refresh on resume and every 30 seconds while the app is resumed, and immediately after a response.
- The screen contains requests, rankings, session registration, and history. Registration is hidden for administrators.
- Linked instructors access agenda management from the calendar toolbar, keeping the bottom navigation at five items.

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

La app ya no usa datos ficticios: consume la API REST del backend Node.js/Fastify de `KungFuBackendService` con sesión por cookie (`wushu_session`).

El backend devuelve `ADMIN` para administradores, `INSTRUCTOR` para instructores y `USER` para alumnos. Android prioriza este rol al resolver el perfil, incluso si un alumno y un instructor comparten ID y nombre.

### Configurar el servidor
- URL por defecto: `http://10.0.2.2:8080` (el `localhost` del PC visto desde el emulador).
- En un móvil físico, abrir **Servidor** en la pantalla de login e indicar la IP del PC (p. ej. `http://192.168.1.50:8080`).
- Se permite tráfico HTTP (`usesCleartextTraffic`) para desarrollo; en producción usar HTTPS.

### Navegación implementada (alineada con la web)
Las pestañas dependen del rol de la sesión:

| Pestaña | Roles | Contenido y acciones |
|---|---|---|
| Inicio | todos | saludo con cinturón y avance de formas obligatorias, mis próximas actividades, actividades de la escuela por día; estadísticas para el administrador |
| Mi ficha | alumnos e instructores con ficha | datos personales, grado, formas obligatorias y opcionales del grado (Sabida / Aprendiendo X % / Pendiente), marcar formas en aprendizaje, guardar % de avance, dejar de aprender, formas sabidas por estilo |
| Calendario | todos | vista Semana / Próximas, filtro por sala; detalle del evento con apuntarse o quitarse (solo ese día o toda la serie), rol alumno/instructor para instructores, contador de asistentes si es visible y lista de participantes para gestores; botón «Sesión» para gestores |
| Agenda | administrador e instructores | programar sesión puntual u horario habitual, horarios agrupados por día de la semana, editar o cancelar sesiones y horarios |
| Cuenta | todos | permisos, servidor, cerrar sesión |

Solo el administrador puede fijar el límite de plazas y si se muestra el número de asistentes. Alumnos, cursos, grados, formas, salas y contraseñas se gestionan desde la web.

### Endpoints usados
- `POST /api/auth/login`, `GET /api/auth/me`, `DELETE /api/auth/logout`
- `GET /api/me/profile`, `PUT` / `DELETE /api/me/forms/{formId}/progress`
- `GET /api/calendar/events?from&to`, `rooms`, `courses`, `instructors`, `recurring-rules`
- `POST` / `DELETE /api/calendar/events/{id}/participation?scope=occurrence|series`, `GET .../participants`
- `POST` / `PATCH /api/calendar/events`, `POST /api/calendar/events/{id}/cancel`
- `POST` / `PATCH /api/calendar/recurring-rules`, `POST /api/calendar/recurring-rules/{id}/cancel`

### Pendiente (no existe aún en el backend)
- Retos y estiramientos (no hay tablas ni endpoints).
- Exámenes / temario de examen.
