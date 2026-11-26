package com.sonora.remote.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sonora.remote.data.SonoraBridgeClient
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SonoraBridgePanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { SonoraBridgeClient(context.applicationContext) }
    var code by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Introduce el código que aparece en SONORA TV") }
    var lyrics by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    SideEffect {
        client.onConnectionChanged = { isConnected, message ->
            scope.launch {
                if (!isConnected) connected = false
                status = message
            }
        }
        client.onIncomingMessage = { message ->
            if (message.optString("type") == "peer-status" && message.optString("role") == "tv") {
                val tvOnline = message.optBoolean("connected")
                scope.launch {
                    connected = tvOnline
                    status = if (tvOnline) "Conectado con SONORA TV" else "Teléfono vinculado · esperando que la TV se conecte"
                }
            }
        }
    }
    val picker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> selectedUri = uri }
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
                }) { Text("Escanear QR (opcional)") }
                OutlinedButton(onClick = {
                    client.disconnect()
                    connected = false
                    status = "Desconectado de SONORA TV"
                }) { Text("Desconectar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { if (!client.sendControl("previous")) status = "No hay conexión con la TV." }, enabled = connected) { Text("Anterior") }
                Button(onClick = { if (!client.sendControl("toggle")) status = "No hay conexión con la TV." }, enabled = connected) { Text("Reproducir / Pausar") }
                OutlinedButton(onClick = { if (!client.sendControl("next")) status = "No hay conexión con la TV." }, enabled = connected) { Text("Siguiente") }
            }
            OutlinedButton(
                onClick = { picker.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(selectedUri?.lastPathSegment ?: "Seleccionar canción del teléfono") }
            OutlinedTextField(
                value = lyrics,
                onValueChange = { lyrics = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Letras (opcional)") },
                minLines = 2,
                maxLines = 4
            )
            OutlinedButton(
                onClick = { status = if (client.sendLyrics(lyrics)) "Letras enviadas a la TV" else "No se pudieron enviar las letras: conexión perdida." },
                enabled = connected && lyrics.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enviar letras") }
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
                            try { client.streamTrack(uri); status = "Transmisión finalizada" }
                            catch (error: Exception) { status = error.message ?: "Error de transmisión" }
                        }
                    },
                    enabled = connected && selectedUri != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Transmitir") }
            }
        }
    }
}
