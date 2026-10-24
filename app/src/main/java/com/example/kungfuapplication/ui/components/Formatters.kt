package com.example.kungfuapplication.ui.components

import androidx.compose.ui.graphics.Color
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es-ES")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", spanish)
private val shortDayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", spanish)
private val longDateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", spanish)
private val fullDayFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanish)
private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", spanish)
private val weekdayInitialFormatter = DateTimeFormatter.ofPattern("EEE", spanish)
private val monthShortFormatter = DateTimeFormatter.ofPattern("MMM", spanish)
private val numericDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", spanish)

fun LocalDateTime.formatTime(): String = format(timeFormatter)
fun LocalTime.formatTime(): String = format(timeFormatter)
fun LocalDate.formatNumeric(): String = format(numericDateFormatter)
fun timeRange(start: LocalDateTime, end: LocalDateTime): String =
    if (start.toLocalDate() == end.toLocalDate()) "${start.formatTime()} – ${end.formatTime()}"
    else "${start.toLocalDate().formatShortDay()} ${start.formatTime()} – ${end.toLocalDate().formatShortDay()} ${end.formatTime()}"
fun LocalDate.formatShortDay(): String = format(shortDayFormatter).capitalizeFirst()
fun LocalDate.formatLong(): String = format(longDateFormatter)
fun LocalDate.formatFullDay(): String = format(fullDayFormatter).capitalizeFirst()
fun LocalDate.formatMonthYear(): String = format(monthYearFormatter).capitalizeFirst()
fun LocalDate.formatWeekday(): String = format(weekdayInitialFormatter).trimEnd('.').capitalizeFirst()
fun LocalDate.formatMonthShort(): String = format(monthShortFormatter).trimEnd('.').uppercase(spanish)

/** "Hoy", "Mañana" o la fecha corta. */
fun LocalDate.relativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Hoy"
    today.plusDays(1) -> "Mañana"
    else -> formatShortDay()
}

fun LocalDate.ageInYears(today: LocalDate = LocalDate.now()): Int = Period.between(this, today).years

fun String.capitalizeFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(spanish) else it.toString() }

fun initialsOf(name: String): String = name.trim()
    .split(Regex("\\s+"))
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "KF" }

/** Traduce el texto libre de `grados.color_cinturon` a un color visible. */
fun beltColorFor(name: String?): Color {
    val key = Normalizer.normalize(name.orEmpty().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
    return when {
        "blanc" in key || "white" in key -> Color(0xFFF5F5F5)
        "amarill" in key || "yellow" in key -> Color(0xFFFFD600)
        "naranj" in key || "orange" in key -> Color(0xFFFF8F00)
        "verde" in key || "green" in key -> Color(0xFF2E7D32)
        "azul" in key || "blue" in key -> Color(0xFF1565C0)
        "morad" in key || "purp" in key || "viol" in key -> Color(0xFF6A1B9A)
        "roj" in key || "red" in key -> Color(0xFFC62828)
        "marr" in key || "cafe" in key || "brown" in key -> Color(0xFF6D4C41)
        "gris" in key || "gray" in key || "grey" in key -> Color(0xFF9E9E9E)
        "negr" in key || "black" in key -> Color(0xFF212121)
        "dorad" in key || "oro" in key || "gold" in key -> Color(0xFFD4AF37)
        else -> Color(0xFF8D6E63)
    }
}
