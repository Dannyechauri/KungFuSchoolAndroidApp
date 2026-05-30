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