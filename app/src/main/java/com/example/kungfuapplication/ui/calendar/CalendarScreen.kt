package com.example.kungfuapplication.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.weekdayShortNames
import com.example.kungfuapplication.ui.AppViewModel
import com.example.kungfuapplication.ui.CalendarMode
import com.example.kungfuapplication.ui.CalendarUiState
import com.example.kungfuapplication.ui.UPCOMING_DAYS
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.ErrorState
import com.example.kungfuapplication.ui.components.EventCard
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.components.formatFullDay
import com.example.kungfuapplication.ui.components.formatMonthShort
import com.example.kungfuapplication.ui.components.formatTime
import com.example.kungfuapplication.ui.components.relativeLabel
import com.example.kungfuapplication.ui.schoolToday
import com.example.kungfuapplication.ui.weekStart
import java.time.LocalDate

@Composable
fun CalendarScreen(user: SessionUser, viewModel: AppViewModel, onOpenEvent: (Long) -> Unit) {
    val state by viewModel.calendar.collectAsStateWithLifecycle()

    if (!state.loaded) {
        if (state.error != null) ErrorState(state.error!!, onRetry = viewModel::reloadEvents)
        else LoadingState(message = "Cargando el calendario…")
        return
    }

    val today = schoolToday()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Espacio inferior para que el botón flotante no tape la última tarjeta.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CalendarMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { viewModel.setCalendarMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, CalendarMode.entries.size),
                    ) { Text(if (mode == CalendarMode.WEEK) "Semana" else "Próximas") }
                }
            }
        }
        if (state.rooms.size > 1) {
            item { RoomFilter(state, viewModel::setRoomFilter) }
        }
        if (state.isLoading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        state.cancellationNotificationError?.let { error ->
            item { Text("No se pudieron cargar los avisos: $error", color = MaterialTheme.colorScheme.error) }
        }
        items(state.cancellationNotifications, key = { "cancel-notice-${it.id}" }) { notification ->
            androidx.compose.material3.ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sesión cancelada", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    Text(notification.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${notification.start.toLocalDate().formatFullDay()} · ${notification.start.toLocalTime().formatTime()}–${notification.end.toLocalTime().formatTime()} · ${notification.roomName ?: "Sala sin asignar"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { viewModel.acknowledgeCancellationNotification(notification.id) },
                        enabled = notification.id !in state.acknowledgingCancellationIds,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (notification.id in state.acknowledgingCancellationIds) "Confirmando…" else "Confirmar que la vi")
                    }
                }
            }
        }
        state.eventNotificationError?.let { error ->
            item { Text("No se pudieron cargar las solicitudes: $error", color = MaterialTheme.colorScheme.error) }
        }
        items(state.eventNotifications.requests, key = { "event-request-${it.eventId}" }) { request ->
            val busy = request.eventId in state.busyEventRequestIds
            androidx.compose.material3.ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Solicitud como instructor", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(request.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${request.start.toLocalDate().formatFullDay()} · ${request.start.toLocalTime().formatTime()}–${request.end.toLocalTime().formatTime()} · ${request.roomName ?: "Sala sin asignar"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${request.organizerName ?: "La administración"} te ha asignado esta actividad. Los alumnos no la verán hasta que la aceptes; si la rechazas, se eliminará y se avisará a quien la creó.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    request.description?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { viewModel.respondToEventRequest(request.eventId, true) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                            Text("Aceptar")
                        }
                        TextButton(onClick = { viewModel.respondToEventRequest(request.eventId, false) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                            Text("Rechazar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        items(state.eventNotifications.responses, key = { "event-response-${it.id}" }) { notice ->
            androidx.compose.material3.ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (notice.accepted) "Actividad aceptada" else "Actividad rechazada",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (notice.accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(notice.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${notice.start.toLocalDate().formatFullDay()} · ${notice.start.toLocalTime().formatTime()}–${notice.end.toLocalTime().formatTime()} · ${notice.roomName ?: "Sala sin asignar"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (notice.accepted) "${notice.instructorName} ha aceptado la actividad y ya es visible para los alumnos."
                        else "${notice.instructorName} ha rechazado la actividad y se ha eliminado.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { viewModel.acknowledgeEventResponse(notice.id) },
                        enabled = notice.id !in state.acknowledgingEventResponseIds,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (notice.id in state.acknowledgingEventResponseIds) "Confirmando…" else "Confirmar que lo vi")
                    }
                }
            }
        }
        state.error?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }

        when (state.mode) {
            CalendarMode.WEEK -> weekContent(state, today, user.instructorId, viewModel::selectDate, onOpenEvent)
            CalendarMode.LIST -> listContent(state.visibleUpcoming, today, user.instructorId, onOpenEvent)
        }
    }
}

@Composable
private fun RoomFilter(state: CalendarUiState, onSelect: (Long?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(selected = state.roomId == null, onClick = { onSelect(null) }, label = { Text("Todas las salas") })
        }
        items(state.rooms.filter { it.active || it.id == state.roomId }, key = { it.id }) { room ->
            FilterChip(selected = state.roomId == room.id, onClick = { onSelect(room.id) }, label = { Text(room.name) })
        }
    }
}

private fun LazyListScope.weekContent(
    state: CalendarUiState,
    today: LocalDate,
    instructorId: Long?,
    onSelectDate: (LocalDate) -> Unit,
    onOpenEvent: (Long) -> Unit,
) {
    val selected = state.selectedDate
    val weekStart = selected.weekStart()
    val loadedWeek = state.weekStart == weekStart
    val weekEvents = if (loadedWeek) state.visibleWeekEvents else emptyList()
    val daysWithEvents = weekEvents.filter { it.active }.map { it.start.toLocalDate() }.toSet()

    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelectDate(selected.minusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
            }
            val end = weekStart.plusDays(6)
            Text(
                "${weekStart.dayOfMonth} ${weekStart.formatMonthShort().lowercase()} – ${end.dayOfMonth} ${end.formatMonthShort().lowercase()} ${end.year}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (selected != today) TextButton(onClick = { onSelectDate(today) }) { Text("Hoy") }
            IconButton(onClick = { onSelectDate(selected.plusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Semana siguiente")
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0L..6L).forEach { offset ->
                val day = weekStart.plusDays(offset)
                DayCell(
                    day = day,
                    selected = day == selected,
                    isToday = day == today,
                    hasEvents = day in daysWithEvents,
                    onClick = { onSelectDate(day) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    item {
        Text(
            selected.formatFullDay(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    val dayEvents = weekEvents.filter { it.start.toLocalDate() == selected }
    if (loadedWeek && dayEvents.isEmpty()) {
        item {
            EmptyState(
                icon = Icons.Filled.EventBusy,
                title = "Día libre",
                message = if (state.roomId == null) "No hay actividades este día." else "No hay actividades en esta sala este día.",
            )
        }
    } else {
        items(dayEvents, key = { "week-${it.id}" }) { event ->
            EventCard(
                event = event,
                onClick = { onOpenEvent(event.id) },
                assignedToCurrentInstructor = instructorId != null && event.instructorId == instructorId,
            )
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container = when {
        selected -> scheme.primary
        isToday -> scheme.primaryContainer
        else -> scheme.surfaceContainerLow
    }
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(weekdayShortNames[day.dayOfWeek.value - 1], style = MaterialTheme.typography.labelSmall, color = content)
        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content)
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (hasEvents) (if (selected) scheme.onPrimary else scheme.primary) else Color.Transparent),
        )
    }
}

private fun LazyListScope.listContent(events: List<CalendarEvent>, today: LocalDate, instructorId: Long?, onOpenEvent: (Long) -> Unit) {
    if (events.isEmpty()) {
        item {
            EmptyState(
                icon = Icons.Filled.EventBusy,
                title = "Sin actividades",
                message = "No hay actividades en los próximos $UPCOMING_DAYS días.",
            )
        }
        return
    }
    events.groupBy { it.start.toLocalDate() }.forEach { (date, dayEvents) ->
        item(key = "list-day-$date") {
            Text(
                if (date == today || date == today.plusDays(1)) "${date.relativeLabel(today)} · ${date.formatFullDay()}" else date.formatFullDay(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        items(dayEvents, key = { "list-${it.id}" }) { event ->
            EventCard(
                event = event,
                onClick = { onOpenEvent(event.id) },
                assignedToCurrentInstructor = instructorId != null && event.instructorId == instructorId,
            )
        }
    }
}
