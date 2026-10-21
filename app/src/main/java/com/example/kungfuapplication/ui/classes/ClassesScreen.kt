package com.example.kungfuapplication.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.ui.components.ClassListItem
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.formatFullDay
import com.example.kungfuapplication.ui.components.formatMonthYear
import com.example.kungfuapplication.ui.components.formatWeekday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun ClassesScreen(data: SchoolData, onOpenClass: (Long) -> Unit) {
    val today = remember { LocalDate.now() }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var onlyMine by rememberSaveable { mutableStateOf(false) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val visibleClasses = remember(data, onlyMine) {
        data.classItems.filter { it.scheduled.dateTime != null && (!onlyMine || it.isMine) }
    }
    val classesByDay = remember(visibleClasses) { visibleClasses.groupBy { it.scheduled.dateTime!!.toLocalDate() } }
    val dayClasses = classesByDay[selectedDate].orEmpty()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = !onlyMine, onClick = { onlyMine = false }, label = { Text("Todas") })
            FilterChip(selected = onlyMine, onClick = { onlyMine = true }, label = { Text("Mis clases") })
        }

        WeekHeader(
            weekStart = weekStart,
            onPrevious = { selectedEpochDay = selectedDate.minusWeeks(1).toEpochDay() },
            onNext = { selectedEpochDay = selectedDate.plusWeeks(1).toEpochDay() },
            onToday = { selectedEpochDay = today.toEpochDay() },
        )
        WeekStrip(
            weekStart = weekStart,
            selected = selectedDate,
            today = today,
            daysWithClasses = classesByDay.keys,
            onSelect = { selectedEpochDay = it.toEpochDay() },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    selectedDate.formatFullDay(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (dayClasses.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.EventBusy,
                        title = "Sin clases este día",
                        message = if (onlyMine) "No tienes clases confirmadas este día." else "No hay clases programadas.",
                    )
                }
            } else {
                items(dayClasses, key = { it.scheduled.id }) { item ->
                    ClassListItem(item, onClick = { onOpenClass(item.scheduled.id) }, showDate = false)
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(weekStart: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
        }
        Text(
            weekStart.plusDays(3).formatMonthYear(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onToday) {
            Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Hoy")
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Semana siguiente")
        }
    }
}

@Composable
private fun WeekStrip(
    weekStart: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    daysWithClasses: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (0L until 7L).map { weekStart.plusDays(it) }.forEach { day ->
            val isSelected = day == selected
            val isToday = day == today
            val container = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
            val content = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(container)
                    .clickable { onSelect(day) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(day.formatWeekday().take(3), style = MaterialTheme.typography.labelSmall, color = content)
                Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = content,
                )
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                day !in daysWithClasses -> Color.Transparent
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.primary
                            },
                        ),
                )
            }
        }
    }
    Box(Modifier.height(4.dp))
}
