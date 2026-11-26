package com.sonora.remote.connection

import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.net.Uri
import android.provider.OpenableColumns
import fi.iki.elonen.NanoHTTPD
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class RemoteUiState(
    val host: String = "",
    val pin: String = "------",
    val localUrl: String = "",
    val serverReady: Boolean = false,
    val tvConnected: Boolean = false,
    val hasMediaAccess: Boolean = false,
    val statusMessage: String = "Preparando conexión local…",
    val phoneTitle: String = "No hay música activa",
    val phoneArtist: String = "Reproduce música en tu teléfono",
    val phoneApp: String = "",
    val phonePlaying: Boolean = false,
    val phoneDuration: Long = 0L,
    val phonePosition: Long = 0L,
    val phoneMediaUri: String = "",
    val tvTitle: String = "Sin canción",
    val tvArtist: String = "Biblioteca de Sonora TV",
    val tvPlaying: Boolean = false,
    val volume: Float = 0.7f
)

object SonoraBridge {
    const val HTTP_PORT = 8765
    const val SOCKET_PORT = 8766

    val state = MutableStateFlow(RemoteUiState())

    private var appContext: Context? = null
    private var httpServer: TvHttpServer? = null
    private var socketServer: TvSocketServer? = null
    private var pairedSocket: WebSocket? = null
    private var activeController: MediaController? = null
    private val streams = ConcurrentHashMap<String, StreamSource>()

    @Synchronized
    fun start(context: Context) {
        appContext = context.applicationContext
        if (httpServer != null && socketServer != null) return

        val host = findLocalIpv4()
        val pin = (100000..999999).random().toString()
        state.value = state.value.copy(
            host = host,
            pin = pin,
            localUrl = if (host.isNotBlank()) "http://$host:$HTTP_PORT/?pin=$pin" else "",
            serverReady = false,
            tvConnected = false,
            statusMessage = if (host.isBlank()) {
                "Conecta el teléfono a la misma red Wi‑Fi que el televisor."
            } else {
                "Iniciando el enlace local…"
            }
        )

        try {
            httpServer = TvHttpServer(appContext!!).also {
                it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
            socketServer = TvSocketServer().also { it.start() }
            state.value = state.value.copy(
                serverReady = true,
                statusMessage = "Abre la dirección local en el navegador del televisor."
            )
        } catch (error: Exception) {
            state.value = state.value.copy(
                serverReady = false,
                statusMessage = "No se pudo iniciar la conexión local: ${error.message ?: "error desconocido"}"
            )
        }
    }

    @Synchronized
    fun stop() {
        try { pairedSocket?.close() } catch (_: Exception) { }
        pairedSocket = null
        try { socketServer?.stop(500) } catch (_: Exception) { }
        try { httpServer?.stop() } catch (_: Exception) { }
        socketServer = null
        httpServer = null
        streams.clear()
        state.value = state.value.copy(
            serverReady = false,
            tvConnected = false,
            statusMessage = "Conexión local detenida."
        )
    }

    fun refreshMediaAccess(context: Context) {
        val enabled = try {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        } catch (_: Exception) {
            false
        }
        state.value = state.value.copy(hasMediaAccess = enabled)
    }

    fun setPhoneController(controller: MediaController?) {
        activeController = controller
        val metadata = controller?.metadata
        val playback = controller?.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "No hay música activa"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: if (controller == null) "Reproduce música en tu teléfono" else "Artista no disponible"
        val mediaUri = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI).orEmpty()
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING
        state.value = state.value.copy(
            phoneTitle = title,
            phoneArtist = artist,
            phoneApp = controller?.packageName.orEmpty(),
            phonePlaying = isPlaying,
            phoneDuration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            phonePosition = playback?.position ?: 0L,
            phoneMediaUri = mediaUri
        )
        sendPhoneMetadata()
    }

    fun sendPhoneCommand(action: String, value: Long? = null) {
        val controller = activeController
        if (controller == null) {
            state.value = state.value.copy(statusMessage = "No se detectó un reproductor activo en el teléfono.")
            return
        }
        try {
            when (action) {
                "play" -> controller.transportControls.play()
                "pause" -> controller.transportControls.pause()
                "toggle" -> {
                    if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                }
                "next" -> controller.transportControls.skipToNext()
                "previous" -> controller.transportControls.skipToPrevious()
                "seek" -> controller.transportControls.seekTo(value ?: 0L)
            }
        } catch (error: Exception) {
            state.value = state.value.copy(statusMessage = "El reproductor del teléfono no aceptó esa orden.")
        }
    }

    fun setVolume(value: Float, controlTv: Boolean) {
        val safe = value.coerceIn(0f, 1f)
        if (controlTv && state.value.tvConnected) {
            sendTvCommand("set_volume", JSONObject().put("value", safe))
        } else {
            val audio = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audio != null) {
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (safe * max).toInt(), 0)
            }
        }
        state.value = state.value.copy(volume = safe)
    }

    fun sendTvCommand(action: String, extras: JSONObject = JSONObject()): Boolean {
        val connection = pairedSocket
        if (connection == null || !connection.isOpen || !state.value.tvConnected) {
            state.value = state.value.copy(statusMessage = "Conecta Sonora TV para enviar controles.")
            return false
        }
        return try {
            val message = JSONObject(extras.toString())
                .put("type", "command")
                .put("action", action)
            connection.send(message.toString())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareAudioToTv(context: Context, uri: Uri) {
        if (!state.value.tvConnected) {
            state.value = state.value.copy(statusMessage = "Primero conecta Sonora TV.")
            return
        }
        try {
            val retriever = MediaMetadataRetriever()
            try { retriever.setDataSource(context, uri) } catch (_: Exception) { }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: queryDisplayName(context.contentResolver, uri)
                ?: "Audio del teléfono"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Archivo local"
            val mime = context.contentResolver.getType(uri) ?: "audio/mpeg"
            val token = UUID.randomUUID().toString()
            streams[token] = StreamSource(uri, mime)
            val url = "http://${state.value.host}:$HTTP_PORT/stream/$token"
            val sent = sendTvCommand(
                "load_url",
                JSONObject()
                    .put("url", url)
                    .put("title", title)
                    .put("artist", artist)
                    .put("autoplay", true)
            )
            if (!sent) streams.remove(token)
            state.value = state.value.copy(
                statusMessage = if (sent) "Enviando «$title» al televisor." else "No se pudo enviar el archivo."
            )
            retriever.release()
        } catch (error: Exception) {
            state.value = state.value.copy(
                statusMessage = "No se pudo preparar el audio: ${error.message ?: "archivo no compatible"}"
            )
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    internal fun onSocketOpen(connection: WebSocket) {
        // The socket is not trusted until the six-digit code has been checked.
    }

    internal fun onSocketClosed(connection: WebSocket) {
        if (pairedSocket === connection) {
            pairedSocket = null
            state.value = state.value.copy(
                tvConnected = false,
                statusMessage = "El televisor se desconectó."
            )
        }
    }

    internal fun onSocketMessage(connection: WebSocket, raw: String) {
        val message = try { JSONObject(raw) } catch (_: Exception) { return }
        when (message.optString("type")) {
            "pair" -> {
                if (message.optString("pin") == state.value.pin && state.value.serverReady) {
                    pairedSocket = connection
                    state.value = state.value.copy(
                        tvConnected = true,
                        statusMessage = "Sonora TV está conectado."
                    )
                    connection.send(JSONObject().put("type", "paired").put("device", "Sonora Remote").toString())
                    sendPhoneMetadata()
                } else {
                    connection.send(JSONObject().put("type", "pair_error").put("message", "Código incorrecto.").toString())
                }
            }
            "tv_state" -> {
                if (connection !== pairedSocket) return
                state.value = state.value.copy(
                    tvTitle = message.optString("title", "Sin canción"),
                    tvArtist = message.optString("artist", "Biblioteca de Sonora TV"),
                    tvPlaying = message.optBoolean("playing", false)
                )
            }
            "ping" -> connection.send(JSONObject().put("type", "pong").toString())
        }
    }

    private fun sendPhoneMetadata() {
        val connection = pairedSocket ?: return
        if (!connection.isOpen) return
        val current = state.value
        val payload = JSONObject()
            .put("type", "phone_media")
            .put("title", current.phoneTitle)
            .put("artist", current.phoneArtist)
            .put("app", current.phoneApp)
            .put("playing", current.phonePlaying)
            .put("duration", current.phoneDuration)
            .put("position", current.phonePosition)
            .put("mediaUri", current.phoneMediaUri)
        try { connection.send(payload.toString()) } catch (_: Exception) { }
    }

    private fun findLocalIpv4(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val addresses = interfaces
                .filter { it.isUp && !it.isLoopback && !it.name.startsWith("tun") }
                .flatMap { network -> network.inetAddresses.toList().filterIsInstance<Inet4Address>().map { network.name to it } }
                .filter { (_, address) -> !address.isLoopbackAddress && !address.isLinkLocalAddress }
                .sortedBy { (name, _) -> if (name.startsWith("wlan") || name.startsWith("wifi")) 0 else if (name.startsWith("eth")) 1 else 2 }
            addresses.firstOrNull()?.second?.hostAddress.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private data class StreamSource(val uri: Uri, val mime: String)

    private class TvSocketServer : WebSocketServer(InetSocketAddress("0.0.0.0", SOCKET_PORT)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            SonoraBridge.onSocketOpen(conn)
        }
        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            SonoraBridge.onSocketClosed(conn)
        }
        override fun onMessage(conn: WebSocket, message: String) {
            SonoraBridge.onSocketMessage(conn, message)
        }
        override fun onError(conn: WebSocket?, ex: Exception) {
            SonoraBridge.state.value = SonoraBridge.state.value.copy(statusMessage = "Error en el enlace del televisor.")
        }
        override fun onStart() { }
    }

    private class TvHttpServer(private val context: Context) : NanoHTTPD(HTTP_PORT) {
        override fun serve(session: IHTTPSession): Response {
            val path = session.uri.substringBefore("?")
            if (path == "/" || path == "/index.html") {
                return try {
                    val html = context.assets.open("sonora-tv/index.html")
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                    newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html).apply {
                        addHeader("Cache-Control", "no-store")
                        addHeader("X-Content-Type-Options", "nosniff")
                    }
                } catch (_: Exception) {
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "No se pudo abrir Sonora TV.")
                }
            }
            if (path.startsWith("/stream/")) {
                val token = path.removePrefix("/stream/")
                val source = SonoraBridge.streams[token]
                    ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Audio no encontrado.")
                return streamResponse(session, source)
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Ruta no encontrada.")
        }

        private fun streamResponse(session: IHTTPSession, source: SonoraBridge.StreamSource): Response {
            val resolver = context.contentResolver
            val descriptor = resolver.openAssetFileDescriptor(source.uri, "r")
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Archivo no disponible.")
            val total = descriptor.length
            val rangeHeader = session.headers["range"]
            var start = 0L
            var end = if (total > 0L) total - 1L else -1L
            var status = Response.Status.OK

            if (!rangeHeader.isNullOrBlank() && total > 0L) {
                val match = Regex("bytes=(\\d*)-(\\d*)").find(rangeHeader)
                if (match == null) {
                    descriptor.close()
                    return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "Rango no válido.")
                }
                start = match.groupValues[1].toLongOrNull() ?: 0L
                end = match.groupValues[2].toLongOrNull() ?: (total - 1L)
                if (start >= total || end < start) {
                    descriptor.close()
                    return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "Rango fuera del archivo.").apply {
                        addHeader("Content-Range", "bytes */$total")
                    }
                }
                end = end.coerceAtMost(total - 1L)
                status = Response.Status.PARTIAL_CONTENT
            }

            val input = try { descriptor.createInputStream() } catch (error: Exception) {
                descriptor.close()
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "No se pudo leer el audio.")
            }
            try {
                var skipped = 0L
                while (skipped < start) {
                    val count = input.skip(start - skipped)
                    if (count <= 0L) {
                        if (input.read() == -1) break
                        skipped++
                    } else skipped += count
                }
                val remaining = if (total > 0L) end - start + 1L else -1L
                val bounded = if (remaining >= 0L) LimitedInputStream(input, descriptor, remaining) else ClosingInputStream(input, descriptor)
                val response = if (remaining >= 0L) {
                    newFixedLengthResponse(status, source.mime, bounded, remaining)
                } else {
                    newChunkedResponse(status, source.mime, bounded)
                }
                response.addHeader("Accept-Ranges", "bytes")
                response.addHeader("Cache-Control", "no-store")
                if (status == Response.Status.PARTIAL_CONTENT) {
                    response.addHeader("Content-Range", "bytes $start-$end/$total")
                }
                if (status == Response.Status.OK && total > 0L) {
                    response.addHeader("Content-Length", total.toString())
                }
                return response
            } catch (error: Exception) {
                try { descriptor.close() } catch (_: Exception) { }
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error transmitiendo el audio.")
            }
        }
    }

    private open class ClosingInputStream(
        input: InputStream,
        private val descriptor: android.content.res.AssetFileDescriptor
    ) : FilterInputStream(input) {
        override fun close() {
            try { super.close() } finally { try { descriptor.close() } catch (_: Exception) { } }
        }
    }

    private class LimitedInputStream(
        input: InputStream,
        descriptor: android.content.res.AssetFileDescriptor,
        private var remaining: Long
    ) : ClosingInputStream(input, descriptor) {
        override fun read(): Int {
            if (remaining <= 0) return -1
            val value = super.read()
            if (value >= 0) remaining--
            return value
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0) return -1
            val read = super.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (read > 0) remaining -= read.toLong()
            return read
        }
    }
}
