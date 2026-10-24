package com.example.kungfuapplication.ui.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kungfuapplication.data.model.FormState
import com.example.kungfuapplication.data.model.GradeFormItem
import com.example.kungfuapplication.data.model.KnownForm
import com.example.kungfuapplication.data.model.MyProfile
import com.example.kungfuapplication.data.model.state
import com.example.kungfuapplication.ui.AppViewModel
import com.example.kungfuapplication.ui.components.BeltChip
import com.example.kungfuapplication.ui.components.ErrorState
import com.example.kungfuapplication.ui.components.InfoLine
import com.example.kungfuapplication.ui.components.InitialsAvatar
import com.example.kungfuapplication.ui.components.LoadingState
import com.example.kungfuapplication.ui.components.PillTone
import com.example.kungfuapplication.ui.components.SectionHeader
import com.example.kungfuapplication.ui.components.StatusPill
import com.example.kungfuapplication.ui.components.ageInYears
import com.example.kungfuapplication.ui.components.formatLong
import kotlin.math.roundToInt

@Composable
fun MyProfileScreen(viewModel: AppViewModel) {
    val state by viewModel.profile.collectAsStateWithLifecycle()
    val profile = state.profile

    if (profile == null) {
        if (state.error != null) ErrorState(state.error!!, onRetry = viewModel::reloadProfile)
        else LoadingState(message = "Cargando tu ficha…")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProfileHeader(profile) }
        item { PersonalData(profile) }

        item { SectionHeader("Formas de mi grado") }
        item { GradeSummary(profile) }
        if (profile.gradeForms.isEmpty()) {
            item {
                Text(
                    if (profile.grade == null) "Todavía no tienes un grado asignado." else "Tu grado no tiene formas asignadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        listOf("Obligatorias" to profile.requiredForms, "Opcionales" to profile.optionalForms).forEach { (title, forms) ->
            if (forms.isNotEmpty()) {
                item(key = "title-$title") {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                }
                items(forms, key = { "grade-form-${it.id}" }) { form ->
                    GradeFormCard(
                        form = form,
                        pending = form.id in state.pendingFormIds,
                        onStart = { viewModel.startLearning(form.id, form.name) },
                        onSave = { percent -> viewModel.saveFormProgress(form.id, percent, form.name) },
                        onStop = { viewModel.stopLearning(form.id, form.name) },
                    )
                }
            }
        }

        item { SectionHeader("Formas que sé") }
        if (profile.knownForms.isEmpty()) {
            item {
                Text(
                    "Aún no tienes formas registradas como aprendidas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            profile.knownForms.groupBy { it.style ?: "Sin estilo" }.toSortedMap().forEach { (style, forms) ->
                item(key = "known-$style") { KnownFormsCard(style, forms) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeader(profile: MyProfile) {
    val student = profile.student
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(student.name, size = 64.dp)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BeltChip(profile.grade?.name, profile.grade?.beltColor)
                    if (student.isInstructor) StatusPill("Instructor", PillTone.Warning)
                    if (!student.active) StatusPill("Inactivo", PillTone.Danger)
                }
            }
        }
    }
}

@Composable
private fun PersonalData(profile: MyProfile) {
    val student = profile.student
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mis datos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            student.enrollmentNumber?.let { InfoLine(Icons.Filled.Tag, "Matrícula", it) }
            student.email?.let { InfoLine(Icons.Filled.Email, "Correo", it) }
            student.birthDate?.let { InfoLine(Icons.Filled.Cake, "Nacimiento", "${it.formatLong()} (${it.ageInYears()} años)") }
            student.joinedOn?.let { InfoLine(Icons.Filled.EventAvailable, "Alta en la escuela", it.formatLong()) }
            profile.instructor?.let { instructor ->
                HorizontalDivider()
                Text("Como instructor", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                InfoLine(Icons.Filled.SportsMartialArts, "Especialidad", instructor.specialty ?: "Sin especialidad")
                if (instructor.styles.isNotEmpty()) {
                    InfoLine(Icons.Filled.SportsMartialArts, "Estilos que imparte", instructor.styles.joinToString(", "))
                }
                instructor.hiredOn?.let { InfoLine(Icons.Filled.EventAvailable, "Instructor desde", it.formatLong()) }
            }
        }
    }
}

@Composable
private fun GradeSummary(profile: MyProfile) {
    val required = profile.requiredForms.size
    val known = profile.knownRequiredCount
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            buildString {
                append(profile.grade?.name ?: "Sin grado")
                if (required > 0) append(" · $known de $required obligatorias sabidas")
                if (profile.learningCount > 0) append(" · ${profile.learningCount} en aprendizaje")
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (required > 0) {
            LinearProgressIndicator(progress = { known / required.toFloat() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun GradeFormCard(
    form: GradeFormItem,
    pending: Boolean,
    onStart: () -> Unit,
    onSave: (Int) -> Unit,
    onStop: () -> Unit,
) {
    val savedPercent = form.learning?.percent ?: 0
    // Se reinicia cuando el servidor devuelve un avance distinto.
    var draft by remember(form.id, savedPercent) { mutableFloatStateOf(savedPercent.toFloat()) }
    val draftPercent = (draft / 5f).roundToInt() * 5

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(form.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    form.style?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                when (form.state) {
                    FormState.KNOWN -> StatusPill("Sabida", PillTone.Success)
                    FormState.LEARNING -> StatusPill("Aprendiendo $savedPercent %", PillTone.Primary)
                    FormState.PENDING -> StatusPill("Pendiente")
                }
            }
            form.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (form.state) {
                FormState.KNOWN -> Unit
                FormState.PENDING -> FilledTonalButton(onClick = onStart, enabled = !pending, modifier = Modifier.fillMaxWidth()) {
                    if (pending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Marcar como aprendiendo")
                }
                FormState.LEARNING -> {
                    LinearProgressIndicator(progress = { savedPercent / 100f }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Avance", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text("$draftPercent %", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        valueRange = 0f..100f,
                        steps = 19,
                        enabled = !pending,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onStop, enabled = !pending, modifier = Modifier.weight(1f)) {
                            Text("Dejar de aprender", maxLines = 1)
                        }
                        Button(
                            onClick = { onSave(draftPercent) },
                            enabled = !pending && draftPercent != savedPercent,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (pending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Guardar avance", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnownFormsCard(style: String, forms: List<KnownForm>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(style, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            forms.sortedBy { it.name }.forEach { form ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(form.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    form.learnedOn?.let {
                        Text(it.formatLong(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
