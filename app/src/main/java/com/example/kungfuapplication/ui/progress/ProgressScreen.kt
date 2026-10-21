package com.example.kungfuapplication.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kungfuapplication.data.model.FormStatus
import com.example.kungfuapplication.data.model.Grade
import com.example.kungfuapplication.data.model.MartialForm
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.ui.components.BeltChip
import com.example.kungfuapplication.ui.components.BeltStripe
import com.example.kungfuapplication.ui.components.EmptyState
import com.example.kungfuapplication.ui.components.SectionHeader
import com.example.kungfuapplication.ui.components.formatLong

@Composable
fun ProgressScreen(data: SchoolData) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val isStudent = data.profile.student != null
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(if (isStudent) "Cinturón" else "Grados") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Formas") })
        }
        when (tab) {
            0 -> BeltTab(data)
            else -> FormsTab(data)
        }
    }
}

@Composable
private fun BeltTab(data: SchoolData) {
    val grades = remember(data) { data.grades.sortedBy { it.order } }
    val current = data.currentGrade
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (data.profile.student != null) {
            item { CurrentBeltCard(data) }
        }
        item { SectionHeader("Grados de la escuela") }
        if (grades.isEmpty()) {
            item {
                EmptyState(Icons.Filled.SportsMartialArts, "Sin grados", "La escuela aún no ha definido grados.")
            }
        }
        items(grades, key = { it.id }) { grade ->
            GradeRow(
                grade = grade,
                formsCount = data.formsForGrade(grade.id).size,
                isCurrent = grade.id == current?.id,
                isAchieved = current != null && grade.order < current.order,
            )
        }
    }
}

@Composable
private fun CurrentBeltCard(data: SchoolData) {
    val current = data.currentGrade
    val progress = data.nextGradeProgress
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tu cinturón actual", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (current == null) {
                Text("Sin grado asignado", style = MaterialTheme.typography.headlineSmall)
            } else {
                Text(current.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                BeltStripe(current.beltColor, Modifier.fillMaxWidth(), height = 18.dp)
            }
            when {
                progress != null -> {
                    Text(
                        "Formas para ${progress.nextGrade.name}: ${progress.learned} de ${progress.total}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                }
                current != null -> Text("¡Has alcanzado el grado más alto!", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun GradeRow(grade: Grade, formsCount: Int, isCurrent: Boolean, isAchieved: Boolean) {
    ListItem(
        leadingContent = { BeltStripe(grade.beltColor, Modifier.width(40.dp), height = 14.dp) },
        headlineContent = {
            Text(grade.name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
        },
        supportingContent = {
            val required = maxOf(grade.requiredForms, 0)
            Text("Cinturón ${grade.beltColor.lowercase()} · $required formas requeridas · $formsCount en temario")
        },
        trailingContent = {
            when {
                isCurrent -> BeltChip(grade)
                isAchieved -> Icon(Icons.Filled.CheckCircle, "Superado", tint = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun FormsTab(data: SchoolData) {
    val isStudent = data.profile.student != null
    val groups = remember(data) {
        data.forms
            .groupBy { it.styleId }
            .map { (styleId, forms) ->
                (data.stylesById[styleId]?.name ?: "Estilo $styleId") to forms.sortedWith(
                    compareBy<MartialForm> { form -> data.minimumGradeFor(form.id)?.order ?: Int.MAX_VALUE }
                        .thenBy { it.name },
                )
            }
            .sortedBy { it.first }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isStudent) {
            item {
                val learned = data.myLearnedForms.size
                Text(
                    "Has aprendido $learned de ${data.forms.size} formas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        if (groups.isEmpty()) {
            item { EmptyState(Icons.AutoMirrored.Filled.MenuBook, "Sin formas", "Todavía no hay formas registradas.") }
        }
        groups.forEach { (styleName, forms) ->
            item(key = "style-$styleName") { SectionHeader(styleName) }
            items(forms, key = { it.id }) { form ->
                FormRow(form, data, isStudent)
            }
        }
    }
}

@Composable
private fun FormRow(form: MartialForm, data: SchoolData, isStudent: Boolean) {
    val status = data.formStatus(form)
    val minGrade = data.minimumGradeFor(form.id)
    ListItem(
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                when {
                    !isStudent -> Icon(Icons.Filled.SportsMartialArts, null, tint = MaterialTheme.colorScheme.primary)
                    status is FormStatus.Learned -> Icon(Icons.Filled.CheckCircle, "Aprendida", tint = MaterialTheme.colorScheme.primary)
                    status is FormStatus.Locked -> Icon(Icons.Filled.Lock, "Bloqueada", tint = MaterialTheme.colorScheme.outline)
                    else -> Icon(Icons.Filled.SportsMartialArts, "Disponible", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        headlineContent = { Text(form.name) },
        supportingContent = {
            Column {
                form.description?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Row {
                    val label = when {
                        !isStudent -> minGrade?.let { "Desde ${it.name}" }
                        status is FormStatus.Learned -> status.date?.let { "Aprendida el ${it.formatLong()}" } ?: "Aprendida"
                        status is FormStatus.Locked -> "Se desbloquea en ${status.requiredGrade.name}"
                        else -> "Disponible para aprender"
                    }
                    label?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
    )
}
