package com.example.kungfuapplication.ui.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kungfuapplication.data.model.PerformanceDecision
import com.example.kungfuapplication.data.model.PerformanceForm
import com.example.kungfuapplication.data.model.PerformancePayload
import com.example.kungfuapplication.data.model.PerformanceRecord
import com.example.kungfuapplication.data.model.RankingCategory
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.ui.AppViewModel
import com.example.kungfuapplication.ui.PerformanceUiState
import com.example.kungfuapplication.ui.components.ErrorState
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.components.SectionHeader
import com.example.kungfuapplication.ui.components.formatLong
import com.example.kungfuapplication.ui.components.formatMonthYear
import com.example.kungfuapplication.ui.schoolToday
import java.time.LocalDate

private enum class PerformanceView(val label: String) {
    REQUESTS("Por confirmar"), RANKINGS("Ranking"), SESSION("Sesión"), HISTORY("Historial"),
}

@Composable
fun PerformanceScreen(user: SessionUser, viewModel: AppViewModel) {
    val state by viewModel.performance.collectAsStateWithLifecycle()
    LaunchedEffect(user.userId, user.role) { viewModel.reloadPerformance() }
    PerformanceContent(user, state, viewModel::respondToPerformance, viewModel::savePerformance, viewModel::reloadPerformance)
}

@Composable
fun PerformanceContent(
    user: SessionUser,
    state: PerformanceUiState,
    onRespond: (Long, PerformanceDecision) -> Unit,
    onSave: (PerformancePayload) -> Unit,
    onRetry: () -> Unit,
) {
    if (!state.loaded) {
        if (state.error != null) ErrorState(state.error, onRetry)
        else LoadingState(message = "Cargando desempeño…")
        return
    }
    val views = remember(user) { PerformanceView.entries.filter { it != PerformanceView.SESSION || user.hasProfile } }
    var selectedView by rememberSaveable(user.userId, user.role) { mutableStateOf(PerformanceView.REQUESTS.name) }
    val selectedIndex = views.indexOfFirst { it.name == selectedView }.coerceAtLeast(0)
    Column(Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp) {
            views.forEachIndexed { index, view ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedView = view.name },
                    text = { Text(if (view == PerformanceView.REQUESTS) "${view.label} (${state.inbox.size})" else view.label) },
                )
            }
        }
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        val error = state.error ?: state.requestsError
        if (error != null) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
        }
        when (views[selectedIndex]) {
            PerformanceView.REQUESTS -> PerformanceRequests(user, state, onRespond)
            PerformanceView.RANKINGS -> PerformanceRankingList(user, state)
            PerformanceView.SESSION -> PerformanceSessionForm(user, state, onSave)
            PerformanceView.HISTORY -> PerformanceHistory(user, state.records)
        }
    }
}

@Composable
private fun PerformanceRequests(user: SessionUser, state: PerformanceUiState, onRespond: (Long, PerformanceDecision) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.inbox.isEmpty() && state.requestsError == null) {
            item { Text("No hay actividades pendientes de confirmar.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.inbox, key = { it.id }) { request ->
            val busy = request.id in state.busyRecordIds
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(request.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(request.date.formatLong(), style = MaterialTheme.typography.bodyMedium)
                    ActivityDetails(request.physicalWork, request.cardioWork, request.stretching, request.attendedClass, request.forms)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRespond(request.id, PerformanceDecision.CONFIRMED) }, enabled = !busy) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(if (user.isAdmin) "Aprobar" else "Confirmar", Modifier.padding(start = 4.dp))
                        }
                        OutlinedButton(onClick = { onRespond(request.id, PerformanceDecision.REJECTED) }, enabled = !busy) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Rechazar", Modifier.padding(start = 4.dp))
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PerformanceRankingList(user: SessionUser, state: PerformanceUiState) {
    val rankings = state.rankings ?: return
    var expanded by rememberSaveable(user.userId) { mutableStateOf(emptyList<String>()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text("Ranking mensual", style = MaterialTheme.typography.titleLarge)
            Text(rankings.month.atDay(1).formatMonthYear(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RankingCategory.entries.forEach { category ->
            val entries = rankings.categories[category].orEmpty()
            val isExpanded = category.name in expanded
            item(key = "heading-${category.name}") {
                SectionHeader(category.label)
            }
            items(rankings.visibleEntries(category, user.isAdmin, isExpanded), key = { "${category.name}-${it.studentId}" }) { entry ->
                val current = entry.studentId == user.studentId
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (current) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${entry.rank}.", style = MaterialTheme.typography.labelLarge)
                    Text(entry.name + if (current) " · Tú" else "", Modifier.weight(1f),
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
                    Text("${entry.points} pt", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (user.isAdmin && entries.size > 3) {
                item(key = "expand-${category.name}") {
                    TextButton(
                        onClick = { expanded = if (isExpanded) expanded - category.name else expanded + category.name },
                        modifier = Modifier.semantics { stateDescription = if (isExpanded) "Lista completa" else "Tres primeros puestos" },
                    ) {
                        Icon(if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                        Text(if (isExpanded) "Ver menos" else "Ver todos (${entries.size})")
                    }
                }
            }
            if (entries.isEmpty()) item(key = "empty-${category.name}") { Text("Sin usuarios rankeados.") }
            item(key = "divider-${category.name}") { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        }
    }
}

@Composable
private fun PerformanceHistory(user: SessionUser, records: List<PerformanceRecord>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(if (user.isAdmin || user.canChooseInstructorRole) "Desempeño registrado" else "Mi historial")
            Text("Últimos 100 registros", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (records.isEmpty()) item { Text("Todavía no hay registros de desempeño.") }
        items(records, key = { it.id }) { record ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.isAdmin || user.canChooseInstructorRole) Text(record.studentName, style = MaterialTheme.typography.titleMedium)
                    Text(record.date.formatLong(), fontWeight = FontWeight.SemiBold)
                    Text(record.statusLabel, style = MaterialTheme.typography.labelLarge,
                        color = if (record.rejectedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    ActivityDetails(record.physicalWork, record.cardioWork, record.stretching, record.attendedClass, record.forms.map { it.name })
                    if (record.confirmations.isNotEmpty()) {
                        HorizontalDivider()
                        record.confirmations.forEach { confirmation ->
                            Text("${confirmation.name}: ${confirmation.status.label}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityDetails(physical: Boolean, cardio: Boolean, stretching: Boolean, attendance: Boolean, forms: List<String>) {
    val activities = buildList {
        if (physical) add("Trabajo físico")
        if (cardio) add("Trabajo cardiovascular")
        if (stretching) add("Estiramiento")
        if (attendance) add("Asistencia a clase")
    }
    if (activities.isNotEmpty()) Text(activities.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
    if (forms.isNotEmpty()) Text("Formas: ${forms.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerformanceSessionForm(user: SessionUser, state: PerformanceUiState, onSave: (PerformancePayload) -> Unit) {
    var epochDay by rememberSaveable(user.userId) { mutableLongStateOf(schoolToday().toEpochDay()) }
    val date = LocalDate.ofEpochDay(epochDay)
    val existing = state.records.firstOrNull { it.date == date && it.studentId == user.studentId }
    var physical by rememberSaveable(user.userId, epochDay) { mutableStateOf(existing?.physicalWork ?: false) }
    var cardio by rememberSaveable(user.userId, epochDay) { mutableStateOf(existing?.cardioWork ?: false) }
    var stretching by rememberSaveable(user.userId, epochDay) { mutableStateOf(existing?.stretching ?: false) }
    var attendance by rememberSaveable(user.userId, epochDay) { mutableStateOf(existing?.attendedClass ?: false) }
    var formIds by rememberSaveable(user.userId, epochDay) { mutableStateOf(existing?.forms?.map { it.id }.orEmpty()) }
    var confirmerIds by rememberSaveable(user.userId, epochDay) {
        mutableStateOf(existing?.confirmations?.map { it.studentId }
            ?.filter { id -> state.confirmers.any { it.id == id } }.orEmpty())
    }
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val enabled = !state.isSaving
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader("Registrar sesión")
            OutlinedButton(onClick = { datePickerOpen = true }, enabled = enabled) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Text(date.formatLong(), Modifier.padding(start = 8.dp))
            }
            if (existing != null) Text("Al guardar se reemplazará el desempeño de esta fecha y se reiniciarán sus confirmaciones.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { PerformanceToggle("Trabajo físico", physical, enabled) { physical = it } }
        item { PerformanceToggle("Trabajo cardiovascular", cardio, enabled) { cardio = it } }
        item { PerformanceToggle("Estiramiento", stretching, enabled) { stretching = it } }
        item { PerformanceToggle("Asistí a clase", attendance, enabled) { attendance = it } }
        item {
            SectionHeader("Formas (${formIds.size}/30)")
            OutlinedButton(onClick = { picker = "forms" }, enabled = enabled) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Seleccionar formas", Modifier.padding(start = 4.dp))
            }
        }
        items(state.forms.filter { it.id in formIds }, key = { "form-${it.id}" }) { form ->
            SelectedPerformanceOption("${form.name} · ${form.style}", enabled) { formIds = formIds - form.id }
        }
        item {
            SectionHeader("Confirmantes (${confirmerIds.size}/100)")
            OutlinedButton(onClick = { picker = "confirmers" }, enabled = enabled) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Seleccionar confirmantes", Modifier.padding(start = 4.dp))
            }
        }
        items(state.confirmers.filter { it.id in confirmerIds }, key = { "confirmer-${it.id}" }) { person ->
            SelectedPerformanceOption(person.name, enabled) { confirmerIds = confirmerIds - person.id }
        }
        item {
            if (state.formError != null) Text(state.formError, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { onSave(PerformancePayload(date, physical, cardio, stretching, attendance, formIds, confirmerIds)) },
                enabled = enabled && confirmerIds.size >= 2 && confirmerIds.size <= 100 && formIds.size <= 30,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Save, contentDescription = null)
                Text(if (state.isSaving) "Guardando…" else "Guardar desempeño", Modifier.padding(start = 8.dp))
            }
        }
    }
    if (datePickerOpen) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { epochDay = it / 86_400_000L }
                    datePickerOpen = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { datePickerOpen = false }) { Text("Cancelar") } },
        ) { DatePicker(state = pickerState) }
    }
    if (picker == "forms") {
        PerformanceSelectionDialog(
            title = "Formas", options = state.forms.sortedWith(compareByDescending<PerformanceForm> { it.inCurrentGrade }.thenByDescending { it.known }.thenBy { it.name }).map {
                SelectionOption(it.id, it.name, listOfNotNull(it.style, when {
                    it.known -> "Ya sabida"
                    it.inCurrentGrade && it.optional -> "Optativa de tu grado"
                    it.inCurrentGrade -> "De tu grado"
                    else -> null
                }).joinToString(" · "))
            },
            selected = formIds, limit = 30, onSelected = { formIds = it }, onDismiss = { picker = null },
        )
    } else if (picker == "confirmers") {
        PerformanceSelectionDialog(
            title = "Confirmantes", options = state.confirmers.map { SelectionOption(it.id, it.name,
                if (it.role == "INSTRUCTOR") "Instructor" else "Alumno") },
            selected = confirmerIds, limit = 100, onSelected = { confirmerIds = it }, onDismiss = { picker = null },
        )
    }
}

@Composable
private fun PerformanceToggle(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label, Modifier.padding(start = 12.dp).weight(1f))
    }
}

@Composable
private fun SelectedPerformanceOption(name: String, enabled: Boolean, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove, enabled = enabled) { Icon(Icons.Filled.Close, contentDescription = "Quitar $name") }
    }
}

private data class SelectionOption(val id: Long, val name: String, val detail: String)

@Composable
private fun PerformanceSelectionDialog(
    title: String,
    options: List<SelectionOption>,
    selected: List<Long>,
    limit: Int,
    onSelected: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = options.filter { "${it.name} ${it.detail}".contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title (${selected.size}/$limit)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar") }, singleLine = true)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    if (visible.isEmpty()) item { Text("Sin resultados.") }
                    items(visible, key = { it.id }) { option ->
                        val checked = option.id in selected
                        Row(
                            modifier = Modifier.fillMaxWidth().toggleable(
                                value = checked, enabled = checked || selected.size < limit, role = Role.Checkbox,
                                onValueChange = { onSelected(if (checked) selected - option.id else selected + option.id) },
                            ).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, enabled = checked || selected.size < limit)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(option.name, style = MaterialTheme.typography.bodyMedium)
                                Text(option.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}