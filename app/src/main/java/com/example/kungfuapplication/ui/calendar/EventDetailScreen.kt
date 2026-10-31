package com.example.kungfuapplication.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.CourseType
import com.example.kungfuapplication.data.model.ParticipationRole
import com.example.kungfuapplication.data.model.ParticipationScope
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.attendanceSummary
import com.example.kungfuapplication.data.model.isSeatBlocked
import com.example.kungfuapplication.data.model.myParticipationText
import com.example.kungfuapplication.ui.AppViewModel
import com.example.kungfuapplication.ui.ParticipantsState
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.InfoLine
import com.example.kungfuapplication.ui.components.PillTone
import com.example.kungfuapplication.ui.components.StatusPill
import com.example.kungfuapplication.ui.components.formatFullDay
import com.example.kungfuapplication.ui.components.timeRange
import com.example.kungfuapplication.ui.schoolNow

@Composable
fun EventDetailScreen(
    eventId: Long,
    user: SessionUser,
    viewModel: AppViewModel,
    onEditEvent: (Long) -> Unit,
    onEditRule: (Long) -> Unit,
) {
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    val participants by viewModel.participants.collectAsStateWithLifecycle()
    val event = calendar.findEvent(eventId)

    if (event == null) {
        EmptyState(
            icon = Icons.Filled.EventBusy,
            title = "Actividad no disponible",
            message = "No se encuentra en las fechas cargadas. Vuelve al calendario y ábrela de nuevo.",
        )
        return
    }

    if (user.canManageCalendar) {
        LaunchedEffect(event.id, event.attendees) { viewModel.loadParticipants(event.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EventHeader(event)
        EventInfo(event)
        ParticipationCard(
            event = event,
            user = user,
            busy = event.id in calendar.busyEventIds,
            participants = participants[event.id],
            onJoin = { scope, role -> viewModel.join(event, scope, role) },
            onLeave = { scope -> viewModel.leave(event, scope) },
        )
        if (user.canManageCalendar && event.canModify && event.active) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onEditEvent(event.id) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (event.isRecurring) "Editar esta sesión" else "Editar sesión")
                }
                event.recurringRuleId?.let { ruleId ->
                    OutlinedButton(onClick = { onEditRule(ruleId) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Editar horario habitual")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventHeader(event: CalendarEvent) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(event.courseName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        event.courseTopic?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!event.active) StatusPill("Cancelada", PillTone.Danger)
            if (event.active && event.pending) StatusPill("Pendiente del instructor", PillTone.Neutral)
            if (event.active && event.full) StatusPill("Completo", PillTone.Danger)
            StatusPill(if (event.courseType == CourseType.CURSO_ESPECIAL) "Curso especial" else "Clase habitual", PillTone.Neutral)
            if (event.myParticipation != null && event.active) StatusPill("Apuntado", PillTone.Primary)
        }
    }
}

@Composable
private fun EventInfo(event: CalendarEvent) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            InfoLine(Icons.Filled.Event, "Fecha", event.start.toLocalDate().formatFullDay())
            InfoLine(Icons.Filled.AccessTime, "Horario", timeRange(event.start, event.end))
            InfoLine(Icons.Filled.MeetingRoom, "Sala", event.roomName ?: "Sala sin asignar")
            InfoLine(Icons.Filled.Person, "Instructor", event.instructorName ?: "Sin instructor")
            val origin = when {
                event.isRecurring && event.modifiedManually -> "Horario habitual · sesión modificada"
                event.isRecurring -> "Horario habitual semanal"
                else -> "Sesión puntual"
            }
            InfoLine(Icons.Filled.Repeat, "Origen", origin)
            event.description?.takeIf(String::isNotBlank)?.let {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Descripción de la sesión", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            event.courseDescription?.takeIf(String::isNotBlank)?.let {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Descripción del curso", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ParticipationCard(
    event: CalendarEvent,
    user: SessionUser,
    busy: Boolean,
    participants: ParticipantsState?,
    onJoin: (ParticipationScope, ParticipationRole) -> Unit,
    onLeave: (ParticipationScope) -> Unit,
) {
    val hasStarted = remember(event.start) { event.hasStarted(schoolNow()) }
    val editable = user.canParticipate && event.active && !hasStarted
    val mine = event.myParticipation
    var scope by rememberSaveable(event.id) { mutableStateOf(ParticipationScope.OCCURRENCE) }
    var role by rememberSaveable(event.id) { mutableStateOf(ParticipationRole.ALUMNO) }
    val effectiveScope = if (event.isRecurring) scope else ParticipationScope.OCCURRENCE
    val seatBlocked = event.isSeatBlocked(role, effectiveScope)

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Participantes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (event.full) StatusPill("Completo", PillTone.Danger)
            }
            Text(event.attendanceSummary(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (user.canManageCalendar && !event.showAttendeeCount) {
                Text(
                    "Los alumnos no ven el número de asistentes de esta actividad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (user.canParticipate && mine != null) {
                Text(event.myParticipationText().orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            if (editable && mine != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { onLeave(ParticipationScope.OCCURRENCE) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (event.isRecurring) "Quitarme de este día" else "Quitarme")
                    }
                    if (event.isRecurring) {
                        OutlinedButton(onClick = { onLeave(ParticipationScope.SERIES) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Quitarme de la serie")
                        }
                    }
                }
            }

            if (editable && mine == null) {
                if (event.isRecurring) {
                    ChoiceGroup(
                        title = "Asistencia",
                        options = listOf(ParticipationScope.OCCURRENCE to "Solo este día", ParticipationScope.SERIES to "Toda la serie"),
                        selected = scope,
                        onSelect = { scope = it },
                    )
                }
                if (user.canChooseInstructorRole) {
                    ChoiceGroup(
                        title = "Participar como",
                        options = listOf(ParticipationRole.ALUMNO to "Alumno", ParticipationRole.INSTRUCTOR to "Instructor"),
                        selected = role,
                        onSelect = { role = it },
                    )
                }
                Button(
                    onClick = { onJoin(effectiveScope, role) },
                    enabled = !busy && !seatBlocked,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Apuntarme")
                }
                if (seatBlocked) {
                    Text("No quedan plazas de alumno en este día.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (user.canParticipate && hasStarted && event.active) {
                Text(
                    "La actividad ya ha empezado; no se puede cambiar la participación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (user.canManageCalendar) {
                HorizontalDivider()
                ParticipantList(participants)
            }
        }
    }
}

@Composable
private fun <T> ChoiceGroup(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.selectableGroup()) {
            options.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected == value, onClick = { onSelect(value) }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == value, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ParticipantList(state: ParticipantsState?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Lista de participantes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            state == null -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            state.participants.isNullOrEmpty() -> Text(
                "Todavía no hay nadie apuntado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> state.participants.forEach { participant ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(participant.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    val label = (if (participant.role == ParticipationRole.INSTRUCTOR) "Instructor" else "Alumno") + if (participant.series) " · serie" else ""
                    StatusPill(label, if (participant.role == ParticipationRole.INSTRUCTOR) PillTone.Warning else PillTone.Neutral)
                }
            }
        }
    }
}
