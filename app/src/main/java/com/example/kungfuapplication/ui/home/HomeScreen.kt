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
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.MyProfile
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.ui.AppViewModel
import com.example.kungfuapplication.ui.components.BeltChip
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.ErrorState
import com.example.kungfuapplication.ui.components.EventCard
import com.example.kungfuapplication.ui.components.InitialsAvatar
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.components.SectionHeader
import com.example.kungfuapplication.ui.components.StatCard
import com.example.kungfuapplication.ui.components.relativeLabel
import com.example.kungfuapplication.ui.schoolNow
import com.example.kungfuapplication.ui.schoolToday

private const val MAX_ITEMS = 4

@Composable
fun HomeScreen(
    user: SessionUser,
    viewModel: AppViewModel,
    onOpenEvent: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    val profileState by viewModel.profile.collectAsStateWithLifecycle()

    if (!calendar.loaded) {
        if (calendar.error != null) ErrorState(calendar.error!!, onRetry = viewModel::refresh)
        else LoadingState(message = "Cargando la agenda…")
        return
    }

    val now = schoolNow()
    val today = schoolToday()
    val upcoming = calendar.upcoming.filter { it.active && it.end.isAfter(now) }
    val mine = upcoming.filter { it.myParticipation != null }
    val school = upcoming.filter { it.myParticipation == null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { GreetingCard(user, profileState.profile, onOpenProfile) }

        if (user.isAdmin) {
            item {
                val todayCount = upcoming.count { it.start.toLocalDate() == today }
                val weekCount = upcoming.count { it.start.toLocalDate().isBefore(today.plusDays(7)) }
                val fullCount = upcoming.count { it.full }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(Icons.Filled.Today, todayCount.toString(), "Hoy", Modifier.weight(1f))
                    StatCard(Icons.Filled.EventAvailable, weekCount.toString(), "7 días", Modifier.weight(1f))
                    StatCard(Icons.Filled.EventBusy, fullCount.toString(), "Completas", Modifier.weight(1f))
                }
            }
        }

        if (user.canParticipate) {
            item { SectionHeader("Mis próximas actividades") }
            if (mine.isEmpty()) {
                item { NoParticipationCard(onOpenCalendar) }
            } else {
                dayGroupedItems(mine.take(MAX_ITEMS), today, onOpenEvent)
            }
        }

        item {
            SectionHeader("Próximas actividades de la escuela") {
                TextButton(onClick = onOpenCalendar) { Text("Ver todo") }
            }
        }
        if (school.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Groups,
                    title = "Sin actividades",
                    message = "No hay actividades programadas en los próximos días.",
                )
            }
        } else {
            dayGroupedItems(school.take(if (user.isAdmin) MAX_ITEMS * 2 else MAX_ITEMS), today, onOpenEvent)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dayGroupedItems(
    events: List<CalendarEvent>,
    today: java.time.LocalDate,
    onOpenEvent: (Long) -> Unit,
) {
    events.groupBy { it.start.toLocalDate() }.forEach { (date, dayEvents) ->
        item(key = "day-$date-${dayEvents.first().id}") {
            Text(
                date.relativeLabel(today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(dayEvents, key = { "home-${it.id}" }) { event ->
            EventCard(event = event, onClick = { onOpenEvent(event.id) })
        }
    }
}

@Composable
private fun GreetingCard(user: SessionUser, profile: MyProfile?, onOpenProfile: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(user.displayName)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hola, ${user.displayName.substringBefore(' ')}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user.roleLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (profile?.grade != null) BeltChip(profile.grade.name, profile.grade.beltColor)
            }
            if (profile != null) {
                val required = profile.requiredForms.size
                val known = profile.knownRequiredCount
                if (required > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "$known de $required formas obligatorias de tu grado",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(progress = { known / required.toFloat() }, modifier = Modifier.fillMaxWidth())
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val learning = profile.learningCount
                    Text(
                        if (learning == 0) "No estás aprendiendo ninguna forma" else "Aprendiendo $learning ${if (learning == 1) "forma" else "formas"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenProfile) { Text("Ver mi ficha") }
                }
            } else if (user.isAdmin) {
                Text(
                    "Alumnos, cursos, grados y formas se gestionan desde la web. Aquí puedes revisar y gestionar la agenda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoParticipationCard(onOpenCalendar: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("No estás apuntado a nada", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Abre una actividad y pulsa «Apuntarme».", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onOpenCalendar) { Text("Calendario") }
        }
    }
}