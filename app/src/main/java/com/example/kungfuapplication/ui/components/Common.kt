package com.example.kungfuapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kungfuapplication.data.model.CalendarEvent
import com.example.kungfuapplication.data.model.CourseType
import com.example.kungfuapplication.data.model.shortAttendance

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Cargando…") {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.CloudOff,
        title = "No se pudieron cargar los datos",
        message = message,
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text("Reintentar") } },
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsOf(name),
            style = if (size >= 72.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Franja de color que representa el cinturón. */
@Composable
fun BeltStripe(colorName: String?, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val color = beltColorFor(colorName)
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(height / 2)),
    )
}

@Composable
fun BeltChip(name: String?, beltColor: String?, modifier: Modifier = Modifier) {
    val color = beltColorFor(beltColor ?: name)
    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color,
        contentColor = onColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            name ?: "Sin grado",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class PillTone { Neutral, Primary, Success, Warning, Danger }

@Composable
fun StatusPill(text: String, tone: PillTone = PillTone.Neutral, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        PillTone.Neutral -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        PillTone.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
        PillTone.Success -> Color(0xFFDCEFD9) to Color(0xFF1B5E20)
        PillTone.Warning -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        PillTone.Danger -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/**
 * Tarjeta de una actividad de la agenda. La columna izquierda muestra la hora (o la fecha si [showDate]),
 * y el borde se resalta cuando el usuario está apuntado.
 */
@Composable
fun EventCard(event: CalendarEvent, onClick: () -> Unit, modifier: Modifier = Modifier, showDate: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val mine = event.myParticipation != null && event.active
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (mine) scheme.primaryContainer.copy(alpha = 0.35f) else scheme.surfaceContainerLow,
        border = if (mine) BorderStroke(1.5.dp, scheme.primary) else null,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (event.active) scheme.secondaryContainer else scheme.surfaceContainerHighest)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showDate) {
                    Text(event.start.toLocalDate().formatMonthShort(), style = MaterialTheme.typography.labelSmall)
                    Text(event.start.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text(event.start.formatTime(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(event.end.formatTime(), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    event.courseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (event.active) null else TextDecoration.LineThrough,
                )
                val details = buildList {
                    if (showDate) add("${event.start.toLocalDate().formatWeekday()} · ${event.start.formatTime()}–${event.end.formatTime()}")
                    event.roomName?.let { add(it) }
                    event.instructorName?.let { add(it) }
                }
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    when {
                        !event.active -> StatusPill("Cancelada", PillTone.Danger)
                        event.full -> StatusPill("Completo", PillTone.Danger)
                    }
                    if (event.courseType == CourseType.CURSO_ESPECIAL) StatusPill("Especial", PillTone.Warning)
                    if (mine) StatusPill("Apuntado", PillTone.Primary)
                }
            }
            val count = event.shortAttendance()
            if (mine) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Estás apuntado", tint = scheme.primary)
            } else if (count != null && !event.full) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = "Asistentes", modifier = Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                    Spacer(Modifier.width(2.dp))
                    Text(count, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun InfoLine(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
