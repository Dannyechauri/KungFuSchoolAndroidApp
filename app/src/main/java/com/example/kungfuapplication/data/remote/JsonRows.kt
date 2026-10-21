package com.example.kungfuapplication.data.remote

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Lectura tolerante de las filas genéricas que devuelve /api/database/tables/{t}/rows. */
internal fun JsonObject.str(key: String): String? {
    val element = this[key]
    if (element == null || element is JsonNull) return null
    return (element as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}

internal fun JsonObject.long(key: String): Long? {
    val raw = str(key) ?: return null
    return raw.toLongOrNull() ?: raw.toDoubleOrNull()?.toLong()
}

internal fun JsonObject.int(key: String): Int? = long(key)?.toInt()

internal fun JsonObject.bool(key: String): Boolean? {
    val element = this[key] as? JsonPrimitive ?: return null
    return element.booleanOrNull ?: element.contentOrNull?.toBooleanStrictOrNull()
}

internal fun JsonObject.date(key: String): LocalDate? = parseBackendDate(str(key))

internal fun JsonObject.dateTime(key: String): LocalDateTime? = parseBackendDateTime(str(key))

private val offsetWithoutColon = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

/**
 * Jackson serializa los TIMESTAMP como ISO con desplazamiento (p. ej. 2026-10-21T16:30:00.000+00:00)
 * o como epoch en milisegundos, según la configuración. Se convierte a la zona horaria del dispositivo,
 * igual que hace la web.
 */
fun parseBackendDateTime(value: String?): LocalDateTime? {
    val s = value?.trim().orEmpty()
    if (s.isEmpty()) return null
    s.toLongOrNull()?.let {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
    }
    runCatching {
        return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }
    runCatching {
        return OffsetDateTime.parse(s, offsetWithoutColon).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }
    runCatching { return LocalDateTime.parse(s.replace(' ', 'T')) }
    runCatching { return LocalDate.parse(s.take(10)).atStartOfDay() }
    return null
}

fun parseBackendDate(value: String?): LocalDate? {
    val s = value?.trim().orEmpty()
    if (s.isEmpty()) return null
    s.toLongOrNull()?.let {
        return Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    if (s.length > 10) {
        parseBackendDateTime(s)?.let { return it.toLocalDate() }
    }
    return runCatching { LocalDate.parse(s.take(10)) }.getOrNull()
}
