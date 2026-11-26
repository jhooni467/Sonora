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

@Composable
fun SonoraBridgePanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { SonoraBridgeClient(context.applicationContext) }
    var server by remember { mutableStateOf("https://") }
    var code by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Conecta tu teléfono con SONORA TV") }
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
            val parts = scanned.trim().split("|", limit = 2)
            if (parts.size != 2) {
                status = "El QR no contiene un código SONORA válido."
                return
            }
            server = parts[0]
            code = parts[1].filter(Char::isDigit).take(6)
        }
        if (code.length != 6) {
            status = "Introduce el código de seis dígitos."
            return
        }
        connected = false
        status = "Vinculando con SONORA TV…"
        scope.launch {
            try {
                client.pair(server, code)
                status = "Servidor conectado · comprobando televisión…"
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
            Text(status, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Servidor SONORA Bridge") },
                singleLine = true
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código de 6 dígitos") },
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
