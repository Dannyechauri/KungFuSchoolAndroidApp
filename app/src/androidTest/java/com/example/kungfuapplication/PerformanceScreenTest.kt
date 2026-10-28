package com.example.kungfuapplication

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kungfuapplication.data.model.PerformanceConfirmer
import com.example.kungfuapplication.data.model.PerformanceDecision
import com.example.kungfuapplication.data.model.PerformancePayload
import com.example.kungfuapplication.data.model.PerformanceRankings
import com.example.kungfuapplication.data.model.PerformanceRecord
import com.example.kungfuapplication.data.model.PerformanceRequest
import com.example.kungfuapplication.data.model.RankingCategory
import com.example.kungfuapplication.data.model.RankingEntry
import com.example.kungfuapplication.data.model.SessionUser
import com.example.kungfuapplication.data.model.UserRole
import com.example.kungfuapplication.ui.PerformanceUiState
import com.example.kungfuapplication.ui.performance.PerformanceContent
import com.example.kungfuapplication.ui.schoolToday
import com.example.kungfuapplication.ui.theme.KungFuApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class PerformanceScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun user(role: UserRole) = SessionUser(role, 1, "Prueba", if (role == UserRole.ADMIN) null else 1, null)

    private fun pending(id: Long = 41) = PerformanceRequest(
        id, "Participante", schoolToday(), true, false, true, false, listOf("Wu Bu Quan"),
    )

    @Test
    fun administratorCanApproveAndRejectWithoutWitnesses() {
        val state = mutableStateOf(PerformanceUiState(loaded = true, inbox = listOf(pending(41), pending(42))))
        val decisions = mutableListOf<Pair<Long, PerformanceDecision>>()
        compose.setContent {
            KungFuApplicationTheme {
                PerformanceContent(user(UserRole.ADMIN), state.value, onRespond = { id, decision ->
                    decisions.add(id to decision)
                    state.value = state.value.copy(inbox = state.value.inbox.filterNot { it.id == id })
                }, onSave = {}, onRetry = {})
            }
        }
        compose.onNodeWithText("Sesión").assertDoesNotExist()
        compose.onAllNodesWithText("Aprobar").onFirst().performClick()
        compose.onNodeWithText("Por confirmar (1)").assertIsDisplayed()
        compose.onNodeWithText("Rechazar").performClick()
        compose.onNodeWithText("Por confirmar (0)").assertIsDisplayed()
        assertEquals(listOf(41L to PerformanceDecision.CONFIRMED, 42L to PerformanceDecision.REJECTED), decisions)
    }

    @Test
    fun administratorCanExpandAndCollapseRanking() {
        val entries = (1..6).map { RankingEntry(it, it.toLong(), "Miembro $it", 10 - it) }
        val state = PerformanceUiState(loaded = true, rankings = PerformanceRankings(YearMonth.of(2026, 10),
            RankingCategory.entries.associateWith { entries }))
        compose.setContent {
            KungFuApplicationTheme { PerformanceContent(user(UserRole.ADMIN), state, { _, _ -> }, {}, {}) }
        }
        compose.onNodeWithText("Ranking").performClick()
        compose.onNodeWithText("Miembro 6").assertDoesNotExist()
        compose.onAllNodesWithText("Ver todos (6)").onFirst().performScrollTo().performClick()
        compose.onAllNodesWithText("Miembro 6").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Ver menos").performScrollTo().performClick()
        compose.onNodeWithText("Miembro 6").assertDoesNotExist()
    }

    @Test
    fun instructorCanConfirmAndSeesAdministrativeDecisionsInHistory() {
        val records = listOf(
            PerformanceRecord(41, 1, "Participante", schoolToday(), "2026-10-28", null, true,
                true, false, false, false, emptyList(), emptyList()),
            PerformanceRecord(42, 1, "Participante", schoolToday().minusDays(1), null, "2026-10-28", true,
                true, false, false, false, emptyList(), emptyList()),
        )
        var response: PerformanceDecision? = null
        compose.setContent {
            KungFuApplicationTheme {
                PerformanceContent(user(UserRole.INSTRUCTOR), PerformanceUiState(loaded = true, inbox = listOf(pending()), records = records),
                    { _, decision -> response = decision }, {}, {})
            }
        }
        compose.onNodeWithText("Aprobar").assertDoesNotExist()
        compose.onNodeWithText("Confirmar").performClick()
        assertEquals(PerformanceDecision.CONFIRMED, response)
        compose.onNodeWithText("Historial").performScrollTo().performClick()
        compose.onNodeWithText("Aprobado por el administrador").assertIsDisplayed()
        compose.onNodeWithText("Rechazado por el administrador").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun studentMustSelectTwoConfirmersBeforeSaving() {
        var saved: PerformancePayload? = null
        val state = PerformanceUiState(loaded = true, confirmers = listOf(
            PerformanceConfirmer(2, "Testigo uno", "ALUMNO"), PerformanceConfirmer(3, "Testigo dos", "INSTRUCTOR"),
        ))
        compose.setContent {
            KungFuApplicationTheme { PerformanceContent(user(UserRole.USER), state, { _, _ -> }, { saved = it }, {}) }
        }
        compose.onNodeWithText("Sesión").performScrollTo().performClick()
        compose.onNodeWithText("Guardar desempeño").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Seleccionar confirmantes").performScrollTo().performClick()
        compose.onNodeWithText("Testigo uno").performClick()
        compose.onNodeWithText("Testigo dos").performClick()
        compose.onNodeWithText("Listo").performClick()
        compose.onNodeWithText("Guardar desempeño").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf(2L, 3L), saved?.confirmerIds)
        assertEquals(schoolToday(), saved?.date)
    }
}