package com.example.kungfuapplication.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kungfuapplication.data.model.RecurringRule
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.weekdayName
import com.example.kungfuapplication.ui.AppViewModel
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.ErrorState
import com.example.kungfuapplication.ui.components.EventCard
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.components.SectionHeader
import com.example.kungfuapplication.ui.components.formatNumeric
import com.example.kungfuapplication.ui.components.formatTime
import com.example.kungfuapplication.ui.schoolNow

@Composable
fun ManageScreen(
    user: SessionUser,
    viewModel: AppViewModel,
    onNewEvent: () -> Unit,
    onNewRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
) {
    val manage by viewModel.manage.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    var ruleToCancel by remember { mutableStateOf<RecurringRule?>(null) }

    LaunchedEffect(Unit) { viewModel.loadManageData() }

    if (!manage.loaded) {
        if (manage.error != null) ErrorState(manage.error!!, onRetry = { viewModel.loadManageData(force = true) })
        else LoadingState(message = "Cargando la agenda…")
        return
    }

    val now = schoolNow()
    val oneOffSessions = calendar.upcoming.filter { it.active && !it.isRecurring && it.end.isAfter(now) }
    val rules = manage.activeRules

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Programar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onNewEvent, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sesión puntual", maxLines = 1)
                        }
                        FilledTonalButton(onClick = onNewRule, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.EventRepeat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Horario habitual", maxLines = 1)
                        }
                    }
                    Text(
                        if (user.isAdmin) "Los cursos y las salas se gestionan desde la web."
                        else "Los límites de plazas, cursos y salas los gestiona administración desde la web.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (manage.isLoading || manage.isSaving) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        manage.formError?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        item { SectionHeader("Horarios habituales") }
        if (rules.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.EventRepeat,
                    title = "Sin horarios",
                    message = "No hay horarios habituales activos.",
                )
            }
        } else {
            item {
                Text(
                    "Pulsa un horario para editarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            rules.groupBy { it.weekday }.toSortedMap().forEach { (weekday, dayRules) ->
                item(key = "rules-$weekday") {
                    WeekdayRulesCard(
                        weekday = weekday,
                        rules = dayRules.sortedBy { it.startTime },
                        enabled = !manage.isSaving,
                        onEdit = { onEditRule(it.id) },
                        onCancel = { ruleToCancel = it },
                    )
                }
            }
        }

        item { SectionHeader("Próximas sesiones puntuales") }
        if (oneOffSessions.isEmpty()) {
            item {
                Text(
                    "No hay sesiones puntuales programadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(oneOffSessions, key = { "oneoff-${it.id}" }) { event ->
                EventCard(
                    event = event,
                    onClick = { onOpenEvent(event.id) },
                    showDate = true,
                    assignedToCurrentInstructor = user.instructorId != null && event.instructorId == user.instructorId,
                )
            }
        }
    }

    ruleToCancel?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToCancel = null },
            title = { Text("Cancelar horario") },
            text = {
                Text("¿Cancelar el horario semanal de «${rule.courseName}» en ${rule.roomName ?: "sala sin asignar"}? También se cancelarán sus sesiones futuras.")
            },
            confirmButton = {
                TextButton(onClick = { ruleToCancel = null; viewModel.cancelRule(rule.id) }) {
                    Text("Cancelar horario", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { ruleToCancel = null }) { Text("Volver") } },
        )
    }
}

@Composable
private fun WeekdayRulesCard(
    weekday: Int,
    rules: List<RecurringRule>,
    enabled: Boolean,
    onEdit: (RecurringRule) -> Unit,
    onCancel: (RecurringRule) -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                weekdayName(weekday),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            rules.forEachIndexed { index, rule ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                RuleRow(rule, enabled, onEdit = { onEdit(rule) }, onCancel = { onCancel(rule) })
            }
        }
    }
}

@Composable
private fun RuleRow(rule: RecurringRule, enabled: Boolean, onEdit: () -> Unit, onCancel: () -> Unit) {
    val extras = buildList {
        rule.participantLimit?.let { add("Límite $it") }
        if (!rule.showAttendeeCount) add("Asistentes ocultos")
        if (rule.startsOn.isAfter(schoolNow().toLocalDate())) add("Desde ${rule.startsOn.formatNumeric()}")
        rule.endsOn?.let { add("Hasta ${it.formatNumeric()}") }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onEdit)
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(52.dp)) {
            Text(rule.startTime.formatTime(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(rule.endTime.formatTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(rule.courseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(rule.roomName ?: "Sala sin asignar", rule.instructorName).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (extras.isNotEmpty()) {
                Text(extras.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        IconButton(onClick = onCancel, enabled = enabled) {
            Icon(Icons.Filled.Delete, contentDescription = "Cancelar horario", tint = MaterialTheme.colorScheme.error)
        }
    }
}