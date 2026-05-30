@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.kungfuapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kungfuapplication.ui.theme.KungFuApplicationTheme
import androidx.compose.material3.LinearProgressIndicator

private enum class AppSection(val title: String, val shortLabel: String) {
    Home("Inicio", "I"),
    Courses("Cursos", "C"),
    Progress("Progreso", "P"),
    Challenges("Retos", "R"),
    Profile("Perfil", "P")
}

private enum class CalendarViewOption(val label: String) {
    Daily("Diario"),
    Weekly("Semanal"),
    Monthly("Mensual"),
    Yearly("Anual")
}

private enum class ProgressTab(val label: String) {
    Forms("Formas"),
    Stretching("Estiramientos"),
    Belt("Cinturón / Examen")
}

@Composable
fun KungFuSchoolApp() {
    var selectedSection by rememberSaveable { mutableStateOf(AppSection.Home) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Escuela de Kung Fu")
                        Text(
                            text = selectedSection.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        icon = {
                            SectionBadge(section.shortLabel, selectedSection == section)
                        },
                        label = { Text(section.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when (selectedSection) {
                AppSection.Home -> HomeScreen()
                AppSection.Courses -> CoursesScreen()
                AppSection.Progress -> ProgressScreen()
                AppSection.Challenges -> ChallengesScreen()
                AppSection.Profile -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun SectionBadge(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeroCard(
            title = "Sofía Ramírez",
            subtitle = "Cinturón verde",
            badge = "Estudiante",
            progress = 0.64f,
            progressLabel = "Progreso semanal"
        )
        DashboardSection(
            title = "Próxima clase",
            description = "Martes, 18:30 • Formas intermedias • Instructor Maestro Chen"
        )
        DashboardSection(
            title = "Reto activo",
            description = "Racha de estiramientos de 30 días • 18 días completados"
        )
        DashboardSection(
            title = "Notificaciones",
            description = "El temario de tu examen de cinturón se ha actualizado."
        )
        MetricRow(
            Metric("Clases", "4 esta semana"),
            Metric("Formas", "7 aprendidas"),
            Metric("Racha", "12 días")
        )
    }
}

@Composable
private fun CoursesScreen() {
    var selectedView by rememberSaveable { mutableStateOf(CalendarViewOption.Weekly) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Calendario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalendarViewOption.entries.forEach { option ->
                FilterChip(
                    selected = selectedView == option,
                    onClick = { selectedView = option },
                    label = { Text(option.label) }
                )
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Resumen semanal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Vista seleccionada: ${selectedView.label}")
                HorizontalDivider()
                TimeSlot("Lun", "19:00", "Estiramientos y bases")
                TimeSlot("Mié", "18:30", "Formas a mano vacía")
                TimeSlot("Sáb", "10:00", "Combate / revisión de reto")
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Inscribirse en el curso")
                }
            }
        }
        InstructorModeCard()
    }
}

@Composable
private fun ProgressScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(ProgressTab.Forms) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            ProgressTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label) }
                )
            }
        }

        when (selectedTab) {
            ProgressTab.Forms -> FormsSection()
            ProgressTab.Stretching -> StretchingSection()
            ProgressTab.Belt -> BeltExamSection()
        }
    }
}

@Composable
private fun ChallengesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Retos activos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        ChallengeCard(
            title = "Estiramiento de 30 días",
            status = "Activo",
            progress = "18/30 días",
            actionLabel = "Continuar"
        )
        ChallengeCard(
            title = "Dominio de formas",
            status = "Disponible",
            progress = "Se desbloquea tras cinturón verde",
            actionLabel = "Unirse"
        )
        ChallengeCard(
            title = "Ejercicios de combate",
            status = "Guiado por instructor",
            progress = "Comienza el próximo mes",
            actionLabel = "Ver detalles"
        )
    }
}

@Composable
private fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeroCard(
            title = "Sofia Ramirez",
            subtitle = "Estudiante • 15 años",
            badge = "Cinturón verde",
            progress = 0.74f,
            progressLabel = "Perfil completado"
        )
        MetricRow(
            Metric("Clases", "42"),
            Metric("Formas", "7"),
            Metric("Exámenes", "2 aprobados")
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Edita tu perfil, datos del cinturón e información de contacto.")
                TextButton(onClick = { }) { Text("Editar perfil") }
            }
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    badge: String,
    progress: Float,
    progressLabel: String
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("KF", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge { Text(badge) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(progressLabel, style = MaterialTheme.typography.labelLarge)
                    Text("${(progress * 100).toInt()}%")
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DashboardSection(title: String, description: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricRow(vararg metrics: Metric) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        metrics.forEach { metric ->
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(metric.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(metric.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TimeSlot(day: String, time: String, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(day, fontWeight = FontWeight.SemiBold)
            Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(title, modifier = Modifier.weight(1f).padding(start = 16.dp))
    }
}

@Composable
private fun InstructorModeCard() {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Instructor mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Crea, edita y programa cursos desde aquí.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("Crear curso") })
                AssistChip(onClick = { }, label = { Text("Editar horario") })
            }
        }
    }
}

@Composable
private fun FormsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormGroup("Mano vacía", "Postura del tigre", "Barrido del dragón", "Flujo de la grulla")
        FormGroup("Estilo", "Bases del norte", "Serie interna")
        FormGroup("Armas", "Forma de bastón", "Forma de lanza", "Forma de espada")
    }
}

@Composable
private fun FormGroup(title: String, vararg items: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    AssistChip(
                        onClick = { },
                        label = { Text(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StretchingSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StretchCard("Frontal", 0.8f, "5/6 sesiones")
        StretchCard("Lateral", 0.55f, "3/6 sesiones")
        StretchCard("Hombros", 0.72f, "4/6 sesiones")
        StretchCard("Espalda", 0.4f, "2/6 sesiones")
    }
}

@Composable
private fun StretchCard(title: String, progress: Float, detail: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BeltExamSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cinturón actual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Cinturón verde", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Formas desbloqueadas: 7")
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Temario del examen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Formas • Sansao • Combate (18-30) • Aplicaciones de formas")
                HorizontalDivider(color = DividerDefaults.color)
                Text("Observaciones del instructor", fontWeight = FontWeight.SemiBold)
                Text("Necesita constancia en los estiramientos semanales.")
            }
        }
    }
}

@Composable
private fun ChallengeCard(title: String, status: String, progress: String, actionLabel: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(status, color = MaterialTheme.colorScheme.primary)
            }
            Text(progress, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { }) {
                Text(actionLabel)
            }
        }
    }
}

private data class Metric(val label: String, val value: String)

@Preview(showBackground = true)
@Composable
private fun KungFuSchoolAppPreview() {
    KungFuApplicationTheme {
        KungFuSchoolApp()
    }
}
