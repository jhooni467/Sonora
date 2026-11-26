package com.sonora.remote.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sonora.remote.connection.SonoraBridge
import com.sonora.remote.data.SonoraBridgeClient
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SonoraBridgePanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { SonoraBridgeClient(context.applicationContext) }
    val phoneState by SonoraBridge.state.collectAsState()
    var code by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Introduce el código que aparece en SONORA TV") }
    var lyrics by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tvTitle by remember { mutableStateOf("Esperando música") }
    var tvArtist by remember { mutableStateOf("SONORA TV") }
    var tvAlbum by remember { mutableStateOf("") }
    var tvArtworkDataUrl by remember { mutableStateOf("") }
    var tvPlaying by remember { mutableStateOf(false) }
    var tvDurationMs by remember { mutableStateOf(0L) }
    var tvPositionMs by remember { mutableStateOf(0L) }
    var tvVolume by remember { mutableStateOf(1f) }
    var lrcStatus by remember { mutableStateOf("Sin archivo LRC cargado") }

    SideEffect {
        client.onConnectionChanged = { isConnected, message ->
            scope.launch {
                if (!isConnected) connected = false
                status = message
            }
        }
        client.onIncomingMessage = { message ->
            when (message.optString("type")) {
                "peer-status" -> if (message.optString("role") == "tv") {
                    val online = message.optBoolean("connected")
                    scope.launch {
                        connected = online
                        status = if (online) "Conectado con SONORA TV" else "El televisor se desconectó"
                    }
                }
                "playback-state" -> {
                    scope.launch {
                        tvTitle = message.optString("title", tvTitle)
                        tvArtist = message.optString("artist", tvArtist)
                        tvAlbum = message.optString("album", tvAlbum)
                        tvArtworkDataUrl = message.optString("artworkDataUrl", tvArtworkDataUrl)
                        tvPlaying = message.optBoolean("playing", tvPlaying)
                        tvDurationMs = message.optLong("durationMs", tvDurationMs)
                        tvPositionMs = message.optLong("positionMs", tvPositionMs)
                        tvVolume = message.optDouble("volume", tvVolume.toDouble()).toFloat().coerceIn(0f, 1f)
                    }
                }
                "metadata" -> {
                    scope.launch {
                        tvTitle = message.optString("title", tvTitle)
                        tvArtist = message.optString("artist", tvArtist)
                        tvAlbum = message.optString("album", tvAlbum)
                        tvArtworkDataUrl = message.optString("artworkDataUrl", tvArtworkDataUrl)
                    }
                }
                "phone-command" -> {
                    val action = message.optString("action")
                    val value = message.optLong("value")
                    scope.launch {
                        when (action) {
                            "volume" -> SonoraBridge.setVolume(message.optDouble("value", 0.7).toFloat(), false)
                            else -> SonoraBridge.sendPhoneCommand(action, value)
                        }
                    }
                }
                "next", "previous" -> {
                    if (message.optString("from") == "tv") {
                        val action = message.optString("type")
                        scope.launch { SonoraBridge.sendPhoneCommand(action) }
                    }
                }
            }
        }
    }

    LaunchedEffect(
        connected,
        phoneState.phoneTitle,
        phoneState.phoneArtist,
        phoneState.phoneAlbum,
        phoneState.phoneArtworkDataUrl,
        phoneState.phoneApp,
        phoneState.phonePlaying,
        phoneState.phoneDuration,
        phoneState.phonePosition
    ) {
        if (connected && phoneState.phoneTitle != "No hay música activa") {
            client.sendPhonePlaybackState(
                title = phoneState.phoneTitle,
                artist = phoneState.phoneArtist,
                album = phoneState.phoneAlbum,
                playing = phoneState.phonePlaying,
                durationMs = phoneState.phoneDuration,
                positionMs = phoneState.phonePosition,
                artworkDataUrl = phoneState.phoneArtworkDataUrl
            )
        }
    }

    val tvArtwork = remember(tvArtworkDataUrl) {
        runCatching {
            val encoded = tvArtworkDataUrl.substringAfter("base64,", "")
            if (encoded.isBlank()) null else {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }

    val audioPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> selectedUri = uri }

    val lrcPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val count = client.sendLyricsFile(uri)
                    lrcStatus = "LRC enviado · $count líneas sincronizadas"
                    status = "Letras sincronizadas enviadas a SONORA TV"
                } catch (error: Exception) {
                    lrcStatus = error.message ?: "No se pudo leer el archivo LRC"
                }
            }
        }
    }

    fun pair(scanned: String? = null) {
        if (scanned != null) {
            val scannedCode = runCatching {
                val payload = JSONObject(scanned)
                if (payload.optString("type") == "sonora-tv-pair") payload.optString("code") else ""
            }.getOrDefault(scanned)
            code = scannedCode.filter(Char::isDigit).take(6)
            if (code.length != 6) {
                status = "El QR no contiene un código SONORA válido."
                return
            }
        }
        if (code.length != 6) {
            status = "Introduce el código de seis dígitos que muestra la TV."
            return
        }
        connected = false
        status = "Conectando con SONORA TV…"
        scope.launch {
            try {
                client.pair(code)
                status = "Código aceptado · comprobando conexión con la TV…"
            } catch (error: Exception) {
                connected = false
                status = error.message ?: "No se pudo conectar"
            }
        }
    }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) pair(result.contents)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF171C26))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("CONECTAR CON SONORA TV", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Text("La TV genera el código. Escríbelo aquí o escanea el QR.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código que aparece en la TV") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { pair() }, enabled = code.length == 6) { Text("Vincular") }
                OutlinedButton(onClick = {
                    scanner.launch(
                        ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("Escanea el QR de SONORA TV")
                            .setBeepEnabled(false)
                            .setOrientationLocked(false)
                    )
                }) { Text("Escanear QR") }
                OutlinedButton(onClick = {
                    client.disconnect()
                    connected = false
                    status = "Desconectado de SONORA TV"
                }) { Text("Desconectar") }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF171C26))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("REPRODUCCIÓN EN SONORA TV", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (tvArtwork != null) {
                    Image(
                        bitmap = tvArtwork.asImageBitmap(),
                        contentDescription = "Portada de $tvTitle",
                        modifier = Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Card(
                        modifier = Modifier.size(76.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF26384A))
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) { androidx.compose.material3.Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary) }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(tvTitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text(tvArtist, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tvAlbum.isNotBlank()) Text(tvAlbum, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    Text(if (tvPlaying) "Reproduciendo" else "En pausa", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                }
            }
            Slider(
                value = if (tvDurationMs > 0) (tvPositionMs.toFloat() / tvDurationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = { fraction ->
                    if (tvDurationMs > 0) {
                        tvPositionMs = (tvDurationMs * fraction).toLong()
                        client.sendControl("seek", tvPositionMs.toDouble())
                    }
                },
                enabled = connected && tvDurationMs > 0
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { if (!client.sendControl("previous")) status = "Conexión perdida" }, enabled = connected) { Text("Anterior") }
                Button(onClick = {
                    val action = if (tvPlaying) "pause" else "play"
                    if (!client.sendControl(action)) status = "Conexión perdida"
                }, enabled = connected) { Text(if (tvPlaying) "Pausar" else "Reproducir") }
                OutlinedButton(onClick = { if (!client.sendControl("next")) status = "Conexión perdida" }, enabled = connected) { Text("Siguiente") }
            }
            Text("VOLUMEN DE SONORA TV · ${(tvVolume * 100).toInt()}%", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = tvVolume,
                onValueChange = { value ->
                    tvVolume = value
                    if (!client.sendControl("volume", value.toDouble())) status = "No se pudo cambiar el volumen"
                },
                enabled = connected
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF171C26))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MÚSICA DEL TELÉFONO", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Text(phoneState.phoneTitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(phoneState.phoneArtist, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (phoneState.phonePlaying) "Reproduciendo en el teléfono" else "Teléfono en pausa", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Activa el acceso multimedia de SONORA para compartir automáticamente la canción que está sonando.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF171C26))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ENVIAR MÚSICA Y LETRAS", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedUri?.lastPathSegment ?: "Seleccionar canción del teléfono")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val uri = selectedUri ?: return@Button
                        scope.launch {
                            try { client.shareFile(uri); status = "Archivo enviado a la TV" }
                            catch (error: Exception) { status = error.message ?: "Error al enviar archivo" }
                        }
                    },
                    enabled = connected && selectedUri != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Enviar archivo") }
                OutlinedButton(
                    onClick = {
                        val uri = selectedUri ?: return@OutlinedButton
                        scope.launch {
                            try { client.streamTrack(uri); status = "Transmisión enviada a SONORA TV" }
                            catch (error: Exception) { status = error.message ?: "Error de transmisión" }
                        }
                    },
                    enabled = connected && selectedUri != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Transmitir") }
            }
            OutlinedButton(
                onClick = { lrcPicker.launch(arrayOf("text/plain", "application/octet-stream")) },
                enabled = connected,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importar archivo de letras .LRC") }
            Text(lrcStatus, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = lyrics,
                onValueChange = { lyrics = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("O escribir letras manualmente") },
                minLines = 2,
                maxLines = 5
            )
            OutlinedButton(
                onClick = {
                    status = if (client.sendLyrics(lyrics)) "Letras enviadas a la TV" else "No se pudieron enviar las letras: conexión perdida."
                },
                enabled = connected && lyrics.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enviar letras manuales") }
        }
    }
}
