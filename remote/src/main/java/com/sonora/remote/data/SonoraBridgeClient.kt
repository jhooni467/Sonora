package com.sonora.remote.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.BufferedSink
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SonoraTrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String,
    val fileName: String,
    val artworkDataUrl: String?
)

/**
 * Client for the shared SONORA Bridge protocol (PROTOCOL.md in SONORA-TV).
 * Select a track with the Android document picker; this avoids broad storage permissions.
 */
class SonoraBridgeClient(private val context: Context) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var socket: WebSocket? = null
    @Volatile private var token: String? = null
    @Volatile private var baseUrl: String = ""

    suspend fun pair(serverUrl: String, sixDigitCode: String, deviceName: String = "SONORA"): Boolean =
        withContext(Dispatchers.IO) {
            val base = normalize(serverUrl)
            require(sixDigitCode.matches(Regex("\\d{6}"))) { "El código debe tener seis dígitos." }
            val payload = JSONObject().put("code", sixDigitCode).put("deviceName", deviceName)
                .toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder().url("$base/api/pair/join").post(payload).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("No se pudo vincular: HTTP ${response.code}")
                val body = JSONObject(response.body?.string().orEmpty())
                token = body.getString("token")
                baseUrl = base
                openSocket()
            }
            true
        }

    private fun openSocket() {
        val currentToken = token ?: return
        val wsUrl = baseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") +
            "/ws?token=$currentToken"
        val request = Request.Builder().url(wsUrl).build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // The UI can subscribe to this callback in a later screen iteration.
            }
        })
    }

    fun sendControl(type: String, value: Double? = null) {
        val message = JSONObject().put("type", type)
        if (value != null) {
            if (type == "seek") message.put("positionMs", value.toLong())
            else message.put("value", value)
        }
        socket?.send(message.toString())
    }

    suspend fun readMetadata(uri: Uri): SonoraTrackMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: displayName(context.contentResolver, uri).substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Artista desconocido"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val picture = retriever.embeddedPicture
            val mime = context.contentResolver.getType(uri) ?: "audio/mpeg"
            val artwork = picture?.let { bytes ->
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$encoded"
            }
            SonoraTrackMetadata(title, artist, album, duration, mime, displayName(context.contentResolver, uri), artwork)
        } finally {
            retriever.release()
        }
    }

    suspend fun shareFile(uri: Uri): SonoraTrackMetadata = withContext(Dispatchers.IO) {
        val meta = readMetadata(uri)
        sendMetadata(meta)
        val currentToken = token ?: throw IOException("Primero conecta SONORA con la TV.")
        val body = uriBody(uri, meta.mimeType)
        val request = Request.Builder().url("$baseUrl/api/upload")
            .header("Authorization", "Bearer $currentToken")
            .header("X-File-Name", meta.fileName)
            .post(body).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("No se pudo enviar el archivo: HTTP ${response.code}")
        }
        meta
    }

    suspend fun streamTrack(uri: Uri): SonoraTrackMetadata = withContext(Dispatchers.IO) {
        val meta = readMetadata(uri)
        sendMetadata(meta)
        val currentToken = token ?: throw IOException("Primero conecta SONORA con la TV.")
        val sessionBody = JSONObject().toString().toRequestBody("application/json".toMediaTypeOrNull())
        val sessionRequest = Request.Builder().url("$baseUrl/api/stream/session")
            .header("Authorization", "Bearer $currentToken")
            .header("X-Track-Title", meta.title)
            .header("X-Track-Artist", meta.artist)
            .header("Content-Type", meta.mimeType)
            .post(sessionBody).build()
        val uploadUrl = http.newCall(sessionRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("No se pudo iniciar la transmisión: HTTP ${response.code}")
            JSONObject(response.body?.string().orEmpty()).getString("uploadUrl")
        }
        val uploadBody = uriBody(uri, meta.mimeType)
        val uploadRequest = Request.Builder().url(baseUrl + uploadUrl)
            .header("Authorization", "Bearer $currentToken")
            .put(uploadBody).build()
        http.newCall(uploadRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Falló la transmisión: HTTP ${response.code}")
        }
        meta
    }

    private fun sendMetadata(meta: SonoraTrackMetadata) {
        val message = JSONObject().put("type", "metadata")
            .put("title", meta.title).put("artist", meta.artist).put("album", meta.album)
            .put("durationMs", meta.durationMs)
        meta.artworkDataUrl?.let { message.put("artworkDataUrl", it) }
        socket?.send(message.toString())
    }

    private fun uriBody(uri: Uri, mime: String): RequestBody = object : RequestBody() {
        override fun contentType() = mime.toMediaTypeOrNull()
        override fun contentLength(): Long = -1
        override fun writeTo(sink: BufferedSink) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    sink.write(buffer, 0, count)
                }
            } ?: throw IOException("No se pudo leer el archivo seleccionado.")
        }
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: "audio"
        }
        return "audio"
    }

    private fun normalize(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        require(trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            "Introduce la dirección HTTPS del servidor SONORA."
        }
        return trimmed
    }

    fun disconnect() {
        socket?.close(1000, "SONORA disconnected")
        socket = null
        token = null
    }
}
