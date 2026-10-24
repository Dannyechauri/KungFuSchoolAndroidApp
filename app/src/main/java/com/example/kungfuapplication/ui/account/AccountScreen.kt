package com.example.kungfuapplication.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.ui.components.InfoLine
import com.example.kungfuapplication.ui.components.InitialsAvatar

@Composable
fun AccountScreen(user: SessionUser, baseUrl: String, onLogout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            InitialsAvatar(user.displayName, size = 88.dp)
            Spacer(Modifier.height(12.dp))
            Text(user.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(user.roleLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                InfoLine(Icons.Filled.Badge, "Permisos", permissionsText(user))
                InfoLine(Icons.Filled.Dns, "Servidor", baseUrl)
                InfoLine(
                    Icons.Filled.Info,
                    "Contraseña",
                    if (user.isAdmin) "Las contraseñas de alumnos e instructores se cambian desde la web, al editar el alumno."
                    else "Si necesitas cambiar tu contraseña, pídeselo a administración.",
                )
            }
        }

        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Quieres cerrar la sesión en este dispositivo?") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } },
        )
    }
}

private fun permissionsText(user: SessionUser): String = when {
    user.isAdmin -> "Gestión completa de la agenda, límites de plazas y visibilidad de asistentes. Alumnos, cursos, grados y formas se gestionan desde la web."
    user.canManageCalendar -> "Puedes consultar tu ficha, apuntarte como alumno o instructor y programar sesiones y horarios."
    else -> "Puedes consultar tu ficha, marcar tu avance en las formas y apuntarte a las actividades."
}
