package com.example.kungfuapplication.ui.calendar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.CourseType
import com.example.kungfuapplication.data.model.EventPayload
import com.example.kungfuapplication.data.model.ParticipationSettings
import com.example.kungfuapplication.data.model.RecurringRule
import com.example.kungfuapplication.data.model.RulePayload
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.weekdayName
import com.example.kungfuapplication.data.model.weekdayShortNames
import com.example.kungfuapplication.ui.AppViewModel
import com.example.kungfuapplication.ui.ManageUiState
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.ErrorState
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.components.formatNumeric
import com.example.kungfuapplication.ui.components.formatTime
import com.example.kungfuapplication.ui.schoolToday
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

data class ScheduleTarget(val kind: String, val id: Long?, val date: LocalDate?) {
    val isRule: Boolean get() = kind == RULE_NEW || kind == RULE_EDIT
    val isEditing: Boolean get() = kind == EVENT_EDIT || kind == RULE_EDIT

    companion object {
        const val EVENT_NEW = "event-new"
        const val EVENT_EDIT = "event-edit"
        const val RULE_NEW = "rule-new"
        const val RULE_EDIT = "rule-edit"
    }
}

/** Resultado de validar el formulario: o un error para mostrar, o el payload listo para enviar. */
sealed interface FormCheck<out T> {
    data class Invalid(val message: String) : FormCheck<Nothing>
    data class Valid<T>(val payload: T) : FormCheck<T>
}

fun checkLimit(unlimited: Boolean, limitText: String): FormCheck<Int?> {
    if (unlimited) return FormCheck.Valid(null)
    val limit = limitText.trim().toIntOrNull()
    if (limit == null || limit < 1) return FormCheck.Invalid("El límite de participantes debe ser un número entero mayor que cero.")
    return FormCheck.Valid(limit)
}

fun checkEvent(
    courseId: Long?, instructorId: Long?, roomId: Long?,
    date: LocalDate, start: LocalTime, end: LocalTime,
    settings: ParticipationSettings?,
    description: String? = null,
): FormCheck<EventPayload> {
    if (courseId == null || instructorId == null || roomId == null) {
        return FormCheck.Invalid("Selecciona un curso, un instructor y una sala activa.")
    }
    if (!end.isAfter(start)) return FormCheck.Invalid("El final debe ser posterior al inicio.")
    return FormCheck.Valid(EventPayload(courseId, instructorId, roomId, LocalDateTime.of(date, start), LocalDateTime.of(date, end), settings, description))
}

fun checkRule(
    courseId: Long?, instructorId: Long?, roomId: Long?,
    weekdays: List<Int>, start: LocalTime, end: LocalTime,
    startsOn: LocalDate, endsOn: LocalDate?,
    settings: ParticipationSettings?,
): FormCheck<RulePayload> {
    if (courseId == null || instructorId == null || roomId == null) {
        return FormCheck.Invalid("Selecciona un curso, un instructor y una sala activa.")
    }
    if (weekdays.isEmpty()) return FormCheck.Invalid("Selecciona al menos un día de la semana.")
    if (!end.isAfter(start)) return FormCheck.Invalid("La hora de fin debe ser posterior a la de inicio.")
    if (endsOn != null && endsOn.isBefore(startsOn)) return FormCheck.Invalid("La fecha final debe ser posterior a la inicial.")
    return FormCheck.Valid(RulePayload(courseId, instructorId, roomId, weekdays.sorted(), start, end, startsOn, endsOn, settings))
}

@Composable
fun ScheduleFormScreen(
    target: ScheduleTarget,
    user: SessionUser,
    viewModel: AppViewModel,
    onClose: () -> Unit,
) {
    val manage by viewModel.manage.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadManageData() }
    DisposableEffect(Unit) { onDispose { viewModel.clearFormError() } }

    if (!manage.loaded) {
        if (manage.error != null) ErrorState(manage.error!!, onRetry = { viewModel.loadManageData(force = true) })
        else LoadingState(message = "Cargando cursos, instructores y salas…")
        return
    }

    val event = if (target.kind == ScheduleTarget.EVENT_EDIT) target.id?.let(calendar::findEvent) else null
    val rule = if (target.kind == ScheduleTarget.RULE_EDIT) manage.rules.firstOrNull { it.id == target.id && it.active } else null
    if ((target.kind == ScheduleTarget.EVENT_EDIT && event == null) || (target.kind == ScheduleTarget.RULE_EDIT && rule == null)) {
        EmptyState(
            icon = Icons.Filled.EventBusy,
            title = "No disponible",
            message = if (target.isRule) "Este horario habitual ya no está activo." else "No se encuentra la sesión en las fechas cargadas.",
            action = { TextButton(onClick = onClose) { Text("Volver") } },
        )
        return
    }

    ScheduleForm(
        target = target,
        event = event,
        rule = rule,
        manage = manage,
        user = user,
        onSubmitEvent = { payload -> viewModel.saveEvent(event?.id, payload, onClose) },
        onSubmitRule = { payload -> viewModel.saveRule(rule?.id, payload, onClose) },
        onCancelEvent = { id -> viewModel.cancelEvent(id, onClose) },
    )
}

@Composable
private fun ScheduleForm(
    target: ScheduleTarget,
    event: CalendarEvent?,
    rule: RecurringRule?,
    manage: ManageUiState,
    user: SessionUser,
    onSubmitEvent: (EventPayload) -> Unit,
    onSubmitRule: (RulePayload) -> Unit,
    onCancelEvent: (Long) -> Unit,
) {
    val currentRoomId = event?.roomId ?: rule?.roomId
    val currentInstructorId = event?.instructorId ?: rule?.instructorId
    val courses = manage.courses.filter { !target.isRule || it.type == CourseType.HABITUAL }
    val instructors = manage.instructors.filter { it.active || it.id == currentInstructorId }
    val rooms = manage.rooms.filter { it.active || it.id == currentRoomId }
    val initialDate = event?.start?.toLocalDate() ?: target.date ?: schoolToday()

    var courseId by rememberSaveable { mutableStateOf(event?.courseId ?: rule?.courseId ?: courses.firstOrNull()?.id) }
    var instructorId by rememberSaveable {
        mutableStateOf(currentInstructorId ?: instructors.firstOrNull { it.id == user.instructorId }?.id ?: instructors.firstOrNull()?.id)
    }
    var roomId by rememberSaveable { mutableStateOf(currentRoomId ?: rooms.firstOrNull()?.id) }
    var date by rememberSaveable { mutableStateOf(initialDate) }
    var startTime by rememberSaveable { mutableStateOf(event?.start?.toLocalTime() ?: rule?.startTime ?: LocalTime.of(18, 0)) }
    var endTime by rememberSaveable { mutableStateOf(event?.end?.toLocalTime() ?: rule?.endTime ?: LocalTime.of(19, 0)) }
    var weekdays by rememberSaveable { mutableStateOf(rule?.let { listOf(it.weekday) } ?: emptyList()) }
    var startsOn by rememberSaveable { mutableStateOf(rule?.startsOn ?: initialDate) }
    var endsOn by rememberSaveable { mutableStateOf(rule?.endsOn) }
    val currentLimit = event?.participantLimit ?: rule?.participantLimit
    var unlimited by rememberSaveable { mutableStateOf(currentLimit == null) }
    var limitText by rememberSaveable { mutableStateOf(currentLimit?.toString().orEmpty()) }
    var showCount by rememberSaveable { mutableStateOf(event?.showAttendeeCount ?: rule?.showAttendeeCount ?: true) }
    var description by rememberSaveable { mutableStateOf(event?.description.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }

    fun submit() {
        validationError = null
        val settings = if (user.canEditParticipation) {
            when (val limit = checkLimit(unlimited, limitText)) {
                is FormCheck.Invalid -> { validationError = limit.message; return }
                is FormCheck.Valid -> ParticipationSettings(limit.payload, showCount)
            }
        } else null
        if (target.isRule) {
            when (val check = checkRule(courseId, instructorId, roomId, weekdays, startTime, endTime, startsOn, endsOn, settings)) {
                is FormCheck.Invalid -> validationError = check.message
                is FormCheck.Valid -> onSubmitRule(check.payload)
            }
        } else {
            when (val check = checkEvent(courseId, instructorId, roomId, date, startTime, endTime, settings, description.trim().ifBlank { null })) {
                is FormCheck.Invalid -> validationError = check.message
                is FormCheck.Valid -> onSubmitEvent(check.payload)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (event?.isRecurring == true) {
            Notice("Este cambio afecta solo a esta sesión, no al resto del horario semanal.")
        }
        if (rule != null) {
            Notice("Los cambios se aplican a las sesiones futuras de este horario que no se hayan modificado a mano.")
        }

        FormSection("Actividad") {
            OptionDropdown("Curso", courses.map { it.id to it.name }, courseId, { courseId = it })
            OptionDropdown("Instructor", instructors.map { it.id to it.name }, instructorId, { instructorId = it })
            OptionDropdown("Sala", rooms.map { it.id to (it.name + if (!it.active) " · Inactiva" else "") }, roomId, { roomId = it })
            if (courses.isEmpty()) ErrorText(if (target.isRule) "No hay clases habituales disponibles." else "No hay cursos disponibles.")
            if (instructors.isEmpty()) ErrorText("No hay instructores disponibles.")
            if (rooms.isEmpty()) ErrorText("No hay salas activas.")
        }

        if (!target.isRule) {
            FormSection("Descripción de la sesión") {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(2000) },
                    label = { Text("Descripción (opcional)") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = { Text("${description.length}/2000") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        FormSection(if (target.isRule) "Horario semanal" else "Fecha y hora") {
            if (target.isRule) {
                Text(
                    if (rule != null) "Día de la semana: ${weekdayName(rule.weekday)}" else "Días de la semana",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (rule == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        weekdayShortNames.forEachIndexed { index, short ->
                            val day = index + 1
                            FilterChip(
                                selected = day in weekdays,
                                onClick = { weekdays = if (day in weekdays) weekdays - day else (weekdays + day).sorted() },
                                label = { Text(short, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                DateField("Fecha", date, onPick = { date = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField("Inicio", startTime, onPick = { startTime = it }, modifier = Modifier.weight(1f))
                TimeField("Fin", endTime, onPick = { endTime = it }, modifier = Modifier.weight(1f))
            }
            if (target.isRule) {
                DateField("Desde", startsOn, onPick = { startsOn = it })
                DateField("Hasta (opcional)", endsOn, onPick = { endsOn = it }, onClear = { endsOn = null })
            }
        }

        if (user.canEditParticipation) {
            FormSection("Participantes") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sin límite de plazas", modifier = Modifier.weight(1f))
                    Switch(checked = unlimited, onCheckedChange = { unlimited = it })
                }
                if (!unlimited) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { value -> limitText = value.filter(Char::isDigit).take(4) },
                        label = { Text("Límite de alumnos") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = showCount, role = Role.Checkbox, onValueChange = { showCount = it })
                        .padding(vertical = 12.dp),
                ) {
                    Checkbox(checked = showCount, onCheckedChange = null)
                    Spacer(Modifier.size(12.dp))
                    Text("Mostrar a los alumnos cuántos asisten", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        (validationError ?: manage.formError)?.let { ErrorText(it) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = ::submit,
                enabled = !manage.isSaving && courses.isNotEmpty() && instructors.isNotEmpty() && rooms.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (manage.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (target.isEditing) "Guardar cambios" else if (target.isRule) "Crear horario" else "Programar sesión")
            }
            if (event != null && event.active) {
                OutlinedButton(
                    onClick = { confirmCancel = true },
                    enabled = !manage.isSaving,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Cancelar sesión") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmCancel && event != null) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancelar sesión") },
            text = { Text("¿Cancelar «${event.courseName}» en ${event.roomName ?: "sala sin asignar"}? La actividad quedará visible como cancelada.") },
            confirmButton = {
                TextButton(onClick = { confirmCancel = false; onCancelEvent(event.id) }) {
                    Text("Cancelar sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Volver") } },
        )
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun Notice(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdown(label: String, options: List<Pair<Long, String>>, selected: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

/** Campo de solo lectura que abre un selector al pulsarlo. */
@Composable
private fun PickerField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) onOpen() }
    }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = if (onClear != null && value.isNotEmpty()) {
            { IconButton(onClick = onClear) { Icon(Icons.Filled.Clear, contentDescription = "Quitar fecha") } }
        } else null,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: LocalDate?, onPick: (LocalDate) -> Unit, onClear: (() -> Unit)? = null) {
    var open by remember { mutableStateOf(false) }
    PickerField(label, value?.formatNumeric().orEmpty(), Icons.Filled.CalendarMonth, onOpen = { open = true }, onClear = onClear)
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: schoolToday()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: LocalTime, onPick: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    PickerField(label, value.formatTime(), Icons.Filled.AccessTime, onOpen = { open = true }, modifier = modifier)
    if (open) {
        val state = rememberTimePickerState(initialHour = value.hour, initialMinute = value.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)); open = false }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}
