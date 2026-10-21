package com.example.kungfuapplication.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
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
import com.example.kungfuapplication.data.model.SchoolData
import com.example.kungfuapplication.ui.components.BeltChip
import com.example.kungfuapplication.ui.components.InitialsAvatar
import com.example.kungfuapplication.ui.components.SectionHeader
import com.example.kungfuapplication.ui.components.StatCard
import com.example.kungfuapplication.ui.components.ageInYears
import com.example.kungfuapplication.ui.components.formatLong
import java.time.LocalDateTime

@Composable
fun ProfileScreen(data: SchoolData, baseUrl: String, onLogout: () -> Unit) {
    val profile = data.profile
    val student = profile.student
    val instructor = profile.instructor
    val now = remember(data) { LocalDateTime.now() }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            InitialsAvatar(profile.displayName, size = 96.dp)
            Spacer(Modifier.height(12.dp))
            Text(profile.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(profile.personType.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (student != null && data.currentGrade != null) {
                Spacer(Modifier.height(8.dp))
                BeltChip(data.currentGrade)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Icons.Filled.History,
                data.attendedClassesCount(now).toString(),
                "Clases asistidas",
                Modifier.weight(1f),
            )
            StatCard(
                Icons.Filled.EventAvailable,
                data.myUpcomingClasses(now).size.toString(),
                "Próximas",
                Modifier.weight(1f),
            )
            if (student != null) {
                StatCard(
                    Icons.Filled.SportsMartialArts,
                    data.myLearnedForms.size.toString(),
                    "Formas",
                    Modifier.weight(1f),
                )
            }
        }

        SectionHeader("Datos personales")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                profile.email?.let { InfoRow(Icons.Filled.Email, "Correo electrónico", it) }
                if (student != null) {
                    student.enrollmentNumber?.let { InfoRow(Icons.Filled.Badge, "Número de matrícula", it) }
                    student.joinDate?.let { InfoRow(Icons.Filled.CalendarToday, "Fecha de ingreso", it.formatLong()) }
                    student.birthDate?.let {
                        InfoRow(Icons.Filled.Cake, "Fecha de nacimiento", "${it.formatLong()} (${it.ageInYears()} años)")
                    }
                    InfoRow(Icons.Filled.WorkspacePremium, "Grado", data.currentGrade?.name ?: "Sin grado asignado")
                    InfoRow(Icons.Filled.CheckCircle, "Estado", if (student.active) "Activo" else "Inactivo")
                }
                if (instructor != null) {
                    instructor.specialty?.let { InfoRow(Icons.Filled.Star, "Especialidad", it) }
                    val styles = data.stylesOfInstructor(instructor.id)
                    if (styles.isNotEmpty()) {
                        InfoRow(Icons.Filled.SportsMartialArts, "Estilos que imparte", styles.joinToString { it.name })
                    }
                    instructor.hireDate?.let { InfoRow(Icons.Filled.CalendarToday, "Fecha de contratación", it.formatLong()) }
                }
                if (student == null && instructor == null) {
                    InfoRow(Icons.Filled.Badge, "Cuenta", "Administrador de la escuela")
                }
            }
        }

        SectionHeader("Aplicación")
        ElevatedCard(Modifier.fillMaxWidth()) {
            InfoRow(Icons.Filled.Cloud, "Servidor", baseUrl)
        }

        OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("Cerrar sesión")
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres cerrar la sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } },
        )
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
