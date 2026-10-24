package com.example.kungfuapplication.data.remote

import com.example.kungfuapplication.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(val status: Int, message: String) : Exception(message) {
    val isUnauthorized: Boolean get() = status == 401
}

/**
 * Cliente HTTP para el backend Node.js/Fastify (KungFuBackendService).
 * La autenticación se basa en la cookie de sesión que devuelve /api/auth/login.
 */
class ApiClient(
    private val settings: AppSettings,
    private val cookieJar: PersistentCookieJar,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val baseUrl: String get() = settings.baseUrl

    suspend fun get(path: String): JsonElement = execute("GET", path, null)

    suspend fun getArray(path: String): List<JsonObject> = get(path).jsonArray.mapNotNull { it as? JsonObject }

    suspend fun post(path: String, body: JsonObject = JsonObject(emptyMap())): JsonElement = execute("POST", path, body)

    suspend fun put(path: String, body: JsonObject): JsonElement = execute("PUT", path, body)

    suspend fun patch(path: String, body: JsonObject): JsonElement = execute("PATCH", path, body)

    suspend fun delete(path: String): JsonElement = execute("DELETE", path, null)

    fun clearSession() = cookieJar.clear()

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private suspend fun execute(method: String, path: String, body: JsonObject?): JsonElement =
        withContext(Dispatchers.IO) {
            val requestBody = body?.toString()?.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(baseUrl + path)
                .header("Accept", "application/json")
                .method(method, requestBody)
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                throw ApiException(0, "No se pudo conectar con el servidor ($baseUrl). Revisa la dirección y que el backend esté en marcha.")
            } catch (e: IllegalArgumentException) {
                throw ApiException(0, "La dirección del servidor no es válida: $baseUrl")
            }

            response.use {
                val text = it.body?.string().orEmpty()
                if (!it.isSuccessful) {
                    throw ApiException(it.code, extractErrorMessage(it.code, text))
                }
                if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text)
            }
        }

    private fun extractErrorMessage(status: Int, body: String): String {
        val serverMessage = runCatching {
            val obj = json.parseToJsonElement(body).jsonObject
            obj["message"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }.getOrNull()
        if (serverMessage != null) return serverMessage
        return when (status) {
            400 -> "Solicitud no válida."
            401 -> "Sesión no válida o credenciales incorrectas."
            403 -> "No tienes permisos para realizar esta acción."
            404 -> "Recurso no encontrado en el servidor."
            409 -> "El registro ya existe."
            in 500..599 -> "Error interno del servidor ($status)."
            else -> "Error HTTP $status"
        }
    }
}
