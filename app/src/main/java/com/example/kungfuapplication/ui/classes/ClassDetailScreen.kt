package com.example.kungfuapplication.ui.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kungfuapplication.data.model.ClassItem
import com.example.kungfuapplication.data.model.PersonType
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.InitialsAvatar
import com.example.kungfuapplication.ui.components.formatFullDay
import com.example.kungfuapplication.ui.components.formatShortDay
import com.example.kungfuapplication.ui.components.formatTime
import java.time.LocalDateTime

@Composable
fun ClassDetailScreen(
    data: SchoolData,
    classId: Long,
    pendingClassIds: Set<Long>,
    onConfirm: (List<Long>) -> Unit,
    onCancel: (classId: Long, attendanceId: Long) -> Unit,
) {
    val item = data.classItem(classId)
    if (item == null) {
        EmptyState(
            icon = Icons.Filled.EventBusy,
            title = "Clase no encontrada",
            message = "Puede que la clase se haya eliminado. Actualiza los datos.",
        )
        return
    }

    val now = remember(data) { LocalDateTime.now() }
    val dateTime = item.scheduled.dateTime
    val isPast = dateTime?.isBefore(now) == true
    val series = remember(data, classId) { data.seriesFrom(item, now) }
    val isPending = item.scheduled.id in pendingClassIds
    val canSeeAttendees = data.profile.personType == PersonType.ADMIN || item.teaching
    var showSeriesDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.course?.topic?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                when {
                    !item.scheduled.active -> AssistChip(onClick = {}, label = { Text("Cancelada") })
                    isPast -> AssistChip(onClick = {}, label = { Text("Finalizada") })
                }
                if (item.enrolled) AssistChip(onClick = {}, label = { Text("Inscrito") })
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                InfoRow(Icons.Filled.CalendarToday, "Fecha", dateTime?.toLocalDate()?.formatFullDay() ?: "Sin fecha")
                InfoRow(Icons.Filled.Schedule, "Hora", dateTime?.formatTime() ?: "–")
                InfoRow(
                    Icons.Filled.School,
                    "Instructor",
                    if (item.teaching) "Tú" else item.instructor?.name ?: "Por asignar",
                )
                InfoRow(Icons.Filled.Groups, "Asistentes confirmados", item.attendees.size.toString())
            }
        }

        item.course?.description?.let { description ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyLarge)
            }
        }

        AttendanceSection(
            item = item,
            isPast = isPast,
            isPending = isPending,
            seriesCount = series.size,
            onConfirmSingle = { onConfirm(listOf(item.scheduled.id)) },
            onConfirmSeries = { showSeriesDialog = true },
            onCancel = { item.myAttendance?.let { onCancel(item.scheduled.id, it.id) } },
        )

        if (canSeeAttendees) {
            HorizontalDivider()
            Text("Lista de asistentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (item.attendees.isEmpty()) {
                Text("Aún no hay asistentes confirmados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                item.attendees.forEach { attendance ->
                    val name = data.attendeeName(attendance)
                    ListItem(
                        leadingContent = { InitialsAvatar(name, size = 40.dp) },
                        headlineContent = { Text(name) },
                        supportingContent = {
                            Text(
                                when {
                                    attendance.studentId != null -> "Alumno"
                                    attendance.instructorId != null -> "Instructor"
                                    else -> "Administrador"
                                },
                            )
                        },
                    )
                }
            }
        }
    }

    if (showSeriesDialog) {
        AlertDialog(
            onDismissRequest = { showSeriesDialog = false },
            title = { Text("Confirmar asistencia") },
            text = {
                Text(
                    "\"${item.title}\" a las ${dateTime?.formatTime().orEmpty()} se repite ${series.size} veces " +
                        "desde el ${dateTime?.toLocalDate()?.formatShortDay().orEmpty()}. ¿A cuáles quieres asistir?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSeriesDialog = false
                    onConfirm(series.filter { it.myAttendance == null }.map { it.scheduled.id })
                }) { Text("Toda la serie") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSeriesDialog = false
                    onConfirm(listOf(item.scheduled.id))
                }) { Text("Solo esta clase") }
            },
        )
    }
}

@Composable
private fun AttendanceSection(
    item: ClassItem,
    isPast: Boolean,
    isPending: Boolean,
    seriesCount: Int,
    onConfirmSingle: () -> Unit,
    onConfirmSeries: () -> Unit,
    onCancel: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (item.myAttendance != null) Icons.Filled.CheckCircle else Icons.Filled.Person,
                    contentDescription = null,
                    tint = if (item.myAttendance != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        item.myAttendance != null -> "Tienes la asistencia confirmada"
                        item.teaching -> "Eres el instructor de esta clase"
                        !item.scheduled.active -> "Esta clase está cancelada"
                        isPast -> "Esta clase ya ha terminado"
                        else -> "Aún no has confirmado tu asistencia"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            when {
                isPending -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Actualizando…")
                }
                item.myAttendance != null -> OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar asistencia")
                }
                !isPast && item.scheduled.active -> Button(
                    onClick = if (seriesCount > 1) onConfirmSeries else onConfirmSingle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Confirmar asistencia")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        overlineContent = { Text(label) },
        headlineContent = { Text(value) },
    )
}
