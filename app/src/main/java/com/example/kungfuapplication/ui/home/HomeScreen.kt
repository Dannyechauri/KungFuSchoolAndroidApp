package com.example.kungfuapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kungfuapplication.data.model.ClassItem
import com.example.kungfuapplication.data.model.PersonType
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.ui.components.BeltChip
import com.example.kungfuapplication.ui.components.ClassListItem
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.InitialsAvatar
import com.example.kungfuapplication.ui.components.SectionHeader
import com.example.kungfuapplication.ui.components.StatCard
import com.example.kungfuapplication.ui.components.formatTime
import com.example.kungfuapplication.ui.components.relativeLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Composable
fun HomeScreen(
    data: SchoolData,
    onOpenClass: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    val now = remember(data) { LocalDateTime.now() }
    val myUpcoming = remember(data) { data.myUpcomingClasses(now) }
    val schoolUpcoming = remember(data) { data.upcomingClasses(now).filterNot { it.isMine }.take(5) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { WelcomeCard(data, onOpenProgress) }
        item { StatsRow(data, now, myUpcoming.size) }

        item { SectionHeader("Tu próxima clase") }
        item {
            val next = myUpcoming.firstOrNull()
            if (next != null) {
                NextClassCard(next, onClick = { onOpenClass(next.scheduled.id) })
            } else {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = Icons.Filled.EventBusy,
                        title = "Sin clases confirmadas",
                        message = "Consulta el calendario y confirma tu asistencia a una clase.",
                        action = { FilledTonalButton(onClick = onOpenCalendar) { Text("Ver calendario") } },
                    )
                }
            }
        }

        if (myUpcoming.size > 1) {
            item { SectionHeader("También tienes") }
            items(myUpcoming.drop(1).take(3), key = { "mine-${it.scheduled.id}" }) { item ->
                ClassListItem(item, onClick = { onOpenClass(item.scheduled.id) })
            }
        }

        item {
            SectionHeader("Próximas clases en la escuela") {
                TextButton(onClick = onOpenCalendar) { Text("Ver todas") }
            }
        }
        if (schoolUpcoming.isEmpty()) {
            item {
                Text(
                    "No hay más clases programadas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(schoolUpcoming, key = { "school-${it.scheduled.id}" }) { item ->
                ClassListItem(item, onClick = { onOpenClass(item.scheduled.id) })
            }
        }
    }
}

@Composable
private fun WelcomeCard(data: SchoolData, onOpenProgress: () -> Unit) {
    val profile = data.profile
    val firstName = profile.displayName.trim().substringBefore(' ')
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(profile.displayName)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hola, $firstName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            append(profile.personType.label)
                            if (profile.personType == PersonType.STUDENT && profile.instructor != null) append(" · Instructor")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (profile.personType == PersonType.STUDENT) {
                    BeltChip(data.currentGrade)
                }
            }
            data.nextGradeProgress?.let { progress ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Hacia ${progress.nextGrade.name}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${progress.learned}/${progress.total} formas", style = MaterialTheme.typography.labelLarge)
                    }
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = onOpenProgress, contentPadding = PaddingValues(0.dp)) {
                        Text("Ver mi progreso")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(data: SchoolData, now: LocalDateTime, myUpcomingCount: Int) {
    val weekEnd = LocalDate.now().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        when (data.profile.personType) {
            PersonType.STUDENT -> {
                StatCard(Icons.Filled.EventAvailable, myUpcomingCount.toString(), "Próximas", Modifier.weight(1f))
                StatCard(Icons.Filled.History, data.attendedClassesCount(now).toString(), "Asistidas", Modifier.weight(1f))
                StatCard(Icons.Filled.SportsMartialArts, data.myLearnedForms.size.toString(), "Formas", Modifier.weight(1f))
            }
            PersonType.INSTRUCTOR -> {
                val teaching = data.upcomingClasses(now).count { it.teaching }
                val thisWeek = data.upcomingClasses(now).count { it.scheduled.dateTime!!.toLocalDate() <= weekEnd }
                StatCard(Icons.Filled.School, teaching.toString(), "Impartes", Modifier.weight(1f))
                StatCard(Icons.Filled.CalendarMonth, thisWeek.toString(), "Esta semana", Modifier.weight(1f))
                StatCard(Icons.Filled.Groups, data.students.count { it.active }.toString(), "Alumnos", Modifier.weight(1f))
            }
            PersonType.ADMIN -> {
                StatCard(Icons.Filled.CalendarMonth, data.upcomingClasses(now).size.toString(), "Clases", Modifier.weight(1f))
                StatCard(Icons.Filled.Groups, data.students.count { it.active }.toString(), "Alumnos", Modifier.weight(1f))
                StatCard(Icons.Filled.School, data.instructors.count { it.active }.toString(), "Instructores", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NextClassCard(item: ClassItem, onClick: () -> Unit) {
    val dateTime = item.scheduled.dateTime
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    dateTime?.let { "${it.toLocalDate().relativeLabel()} · ${it.formatTime()}" } ?: "Sin fecha",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            item.course?.topic?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                buildString {
                    append(if (item.teaching) "Impartes esta clase" else item.instructor?.name?.let { "Instructor: $it" } ?: "Instructor por asignar")
                    append(" · ${item.attendees.size} asistente(s)")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
