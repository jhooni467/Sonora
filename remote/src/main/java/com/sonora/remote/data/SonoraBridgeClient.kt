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
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture

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
        .pingInterval(25, TimeUnit.SECONDS)
        .build()
    private var socket: WebSocket? = null
    @Volatile private var token: String? = null
    @Volatile private var baseUrl: String = ""
    private val pendingMessages = java.util.concurrent.CopyOnWriteArrayList<String>()
    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor { task -> Thread(task, "sonora-tv-reconnect").apply { isDaemon = true } }
    @Volatile private var intentionalDisconnect = false
    @Volatile private var reconnectAttempt = 0
    @Volatile private var reconnectFuture: ScheduledFuture<*>? = null
    @Volatile var onConnectionChanged: ((Boolean, String) -> Unit)? = null
    @Volatile var onIncomingMessage: ((JSONObject) -> Unit)? = null

    private val sharedConfigUrl = "https://jhooni467.github.io/Sonora-TV-/bridge-config.json"

    private fun loadBridgeUrl(): String {
        val request = Request.Builder()
            .url(sharedConfigUrl)
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        http.newCall(request).apply { timeout().timeout(15, TimeUnit.SECONDS) }.execute().use { response ->
            if (!response.isSuccessful) throw IOException("No se pudo cargar la configuración de conexión.")
            val config = JSONObject(response.body?.string().orEmpty())
            val configuredUrl = config.optString("bridgeUrl", "").trim()
            if (configuredUrl.isBlank()) {
                throw IOException("SONORA Bridge todavía no está desplegado. La configuración se activará cuando se publique el servidor.")
            }
            return normalize(configuredUrl)
        }
    }

    suspend fun pair(sixDigitCode: String, deviceName: String = "SONORA"): Boolean =
        withContext(Dispatchers.IO) {
            val base = loadBridgeUrl()
            require(sixDigitCode.matches(Regex("\\d{6}"))) { "El código debe tener seis dígitos." }
            intentionalDisconnect = true
            reconnectFuture?.cancel(false)
            reconnectFuture = null
            socket?.close(1000, "New pairing attempt")
            socket = null
            token = null
            intentionalDisconnect = false
            reconnectAttempt = 0
            onConnectionChanged?.invoke(false, "Vinculando con SONORA TV…")
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

    private fun openSocket(waitForOpen: Boolean = true) {
        if (socket != null) return
        val currentToken = token ?: throw IOException("No se recibió el token de emparejamiento.")
        val wsUrl = baseUrl.replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") + "/ws?token=$currentToken"
        val request = Request.Builder().url(wsUrl).build()
        val opened = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                pendingMessages.forEach { webSocket.send(it) }
                pendingMessages.clear()
                reconnectAttempt = 0
                reconnectFuture = null
                onConnectionChanged?.invoke(true, "Canal seguro conectado con SONORA TV")
                opened.countDown()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    onIncomingMessage?.invoke(JSONObject(text))
                } catch (_: Exception) {
                    // Ignore malformed bridge messages without breaking the connection.
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (socket !== webSocket) return
                socket = null
                onConnectionChanged?.invoke(false, "Conexión cerrada con SONORA TV")
                if (!intentionalDisconnect && code != 4001 && code != 4002) scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (socket !== webSocket) return
                failure.set(t)
                socket = null
                onConnectionChanged?.invoke(false, "Conexión perdida: ${t.message ?: "error de red"}")
                opened.countDown()
                if (!intentionalDisconnect) scheduleReconnect()
            }
        })
        if (waitForOpen) {
            if (!opened.await(12, TimeUnit.SECONDS)) {
                socket?.cancel()
                socket = null
                throw IOException("Tiempo agotado al abrir el canal de control.")
            }
            failure.get()?.let { throw IOException("No se pudo abrir el canal WebSocket: ${it.message}", it) }
        }
    }

    private fun scheduleReconnect() {
        if (intentionalDisconnect || token == null || reconnectFuture?.isDone == false) return
        val delaySeconds = (1L shl reconnectAttempt.coerceAtMost(4)).coerceAtMost(15L)
        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(4)
        reconnectFuture = reconnectExecutor.schedule({
            reconnectFuture = null
            if (!intentionalDisconnect && token != null && socket == null) {
                try {
                    openSocket(waitForOpen = false)
                } catch (error: Exception) {
                    onConnectionChanged?.invoke(false, "No se pudo recuperar la conexión: ${error.message}")
                    scheduleReconnect()
                }
            }
        }, delaySeconds, TimeUnit.SECONDS)
    }

    fun sendControl(type: String, value: Double? = null): Boolean {
        val message = JSONObject().put("type", type)
        if (value != null) {
            if (type == "seek") message.put("positionMs", value.toLong())
            else message.put("value", value)
        }
        return socket?.send(message.toString()) == true
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

    fun sendPhonePlaybackState(
        title: String,
        artist: String,
        album: String,
        playing: Boolean,
        durationMs: Long,
        positionMs: Long
    ): Boolean {
        val message = JSONObject()
            .put("type", "metadata")
            .put("title", title)
            .put("artist", artist)
            .put("album", album)
            .put("playing", playing)
            .put("durationMs", durationMs)
            .put("positionMs", positionMs)
            .put("source", "phone-media-session")
        return socket?.send(message.toString()) == true
    }

    suspend fun sendLyricsFile(uri: Uri): Int = withContext(Dispatchers.IO) {
        val source = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IOException("No se pudo leer el archivo LRC.")
        val content = source.removePrefix("\uFEFF")
        val offset = Regex("(?im)^\\s*\\[offset:([+-]?\\d+)\\]\\s*$")
            .find(content)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val timestamp = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]""")
        val lyricLines = JSONArray()
        val plainLines = mutableListOf<String>()
        content.lineSequence().forEach { raw ->
            val matches = timestamp.findAll(raw).toList()
            if (matches.isEmpty()) {
                val trimmed = raw.trim()
                if (trimmed.isNotEmpty() && !Regex("""^\[(ti|ar|al|by|re|ve|length):""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
                    && !Regex("""^\[offset:""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
                ) plainLines.add(trimmed)
            } else {
                val lyric = raw.substring(matches.last().range.last + 1).trim()
                if (lyric.isNotEmpty()) {
                    plainLines.add(lyric)
                    matches.forEach { match ->
                        val minutes = match.groupValues[1].toLong()
                        val seconds = match.groupValues[2].toLong()
                        val fractionRaw = match.groupValues.getOrNull(3).orEmpty()
                        val fraction = fractionRaw.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                        val startMs = (minutes * 60_000L + seconds * 1_000L + fraction + offset).coerceAtLeast(0L)
                        lyricLines.put(JSONObject().put("startMs", startMs).put("text", lyric))
                    }
                }
            }
        }
        if (plainLines.isEmpty()) throw IOException("El archivo LRC no contiene letras legibles.")
        val payload = JSONObject()
            .put("type", "lyrics")
            .put("format", "lrc")
            .put("text", plainLines.joinToString("\n"))
            .put("lines", lyricLines)
        if (socket?.send(payload.toString()) != true) {
            throw IOException("No se pudieron enviar las letras: conexión con SONORA TV perdida.")
        }
        lyricLines.length()
    }

    fun sendLyrics(text: String): Boolean {
        val message = JSONObject().put("type", "lyrics").put("text", text)
        return socket?.send(message.toString()) == true
    }

    private fun sendMetadata(meta: SonoraTrackMetadata) {
        val message = JSONObject().put("type", "metadata")
            .put("title", meta.title).put("artist", meta.artist).put("album", meta.album)
            .put("durationMs", meta.durationMs)
        meta.artworkDataUrl?.let { message.put("artworkDataUrl", it) }
        val serialized = message.toString()
        if (socket?.send(serialized) != true) pendingMessages.add(serialized)
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
        require(trimmed.startsWith("https://")) {
            "La dirección configurada de SONORA Bridge debe usar HTTPS."
        }
        return trimmed
    }

    fun disconnect() {
        intentionalDisconnect = true
        reconnectFuture?.cancel(false)
        reconnectFuture = null
        socket?.close(1000, "SONORA disconnected")
        socket = null
        token = null
        pendingMessages.clear()
        onConnectionChanged?.invoke(false, "Desconectado de SONORA TV")
    }
}
