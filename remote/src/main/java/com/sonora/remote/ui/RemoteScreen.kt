package com.sonora.remote.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.sonora.remote.connection.RemoteUiState
import com.sonora.remote.connection.SonoraBridge

private val ThemeOptions = listOf(
    "original" to "Original",
    "aurora" to "Aurora",
    "ocean" to "Océano",
    "sunset" to "Atardecer",
    "forest" to "Bosque",
    "rose" to "Rosa"
)
private val LyricsAnimationOptions = listOf(
    "actual" to "Actual",
    "recorrer" to "Recorrer",
    "fluida" to "Fluida"
)

@Composable
fun RemoteScreen(
    onChooseAudio: () -> Unit,
    onOpenMediaAccess: () -> Unit,
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    lyricsAnimation: String,
    onLyricsAnimationChange: (String) -> Unit
) {
    val ui by SonoraBridge.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        Header()
        SonoraBridgePanel(selectedTheme = selectedTheme, lyricsAnimation = lyricsAnimation)
        PersonalizationCard(
            selectedTheme = selectedTheme,
            onThemeChange = onThemeChange,
            lyricsAnimation = lyricsAnimation,
            onLyricsAnimationChange = onLyricsAnimationChange
        )
        if (!ui.hasMediaAccess) MediaAccessCard(onOpenMediaAccess = onOpenMediaAccess)
        Text(
            "SONORA · CONTROL REMOTO SINCRONIZADO",
            modifier = Modifier.fillMaxWidth(),
            color = androidx.compose.ui.graphics.Color(0xFF737D8E),
            fontSize = 9.sp,
            letterSpacing = 1.5.sp
        )
    }
}


@Composable
private fun PersonalizationCard(
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    lyricsAnimation: String,
    onLyricsAnimationChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text("AJUSTES · PERSONALIZACIÓN", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("TEMAS DE COLOR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, letterSpacing = 1.2.sp)
            ThemeOptions.chunked(3).forEach { group ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    group.forEach { (id, title) ->
                        if (selectedTheme == id) {
                            Button(
                                onClick = { onThemeChange(id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(title, maxLines = 1) }
                        } else {
                            OutlinedButton(
                                onClick = { onThemeChange(id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(title, maxLines = 1) }
                        }
                    }
                }
            }
            Text("ANIMACIÓN DE LETRAS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, letterSpacing = 1.2.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                LyricsAnimationOptions.forEach { (id, title) ->
                    if (lyricsAnimation == id) {
                        Button(
                            onClick = { onLyricsAnimationChange(id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(title, maxLines = 1) }
                    } else {
                        OutlinedButton(
                            onClick = { onLyricsAnimationChange(id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(title, maxLines = 1) }
                    }
                }
            }
            Text(
                "La TV adopta el tema y la animación elegidos mientras el teléfono esté conectado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("SONORA", color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("CONTROL UNIVERSAL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Surface(
            shape = CircleShape,
            color = androidx.compose.ui.graphics.Color(0xFF242936)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Outlined.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("CÓDIGO DE TV", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun ConnectionCard(state: RemoteUiState, onPair: () -> Unit, onChooseAudio: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(androidx.compose.ui.graphics.Color(0xFF252D3B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(27.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Televisor", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        if (state.tvConnected) state.tvTitle else "Conecta Sonora TV en la misma Wi‑Fi",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onPair,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = androidx.compose.ui.graphics.Color(0xFF211307))
                ) {
                    Text(if (state.tvConnected) "Ver enlace" else "Conectar")
                }
            }
            if (state.tvConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFF142D28)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Conectado · " + state.tvArtist, color = androidx.compose.ui.graphics.Color(0xFF9DE2C3), fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("PIN de emparejamiento", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(state.pin.chunked(3).joinToString(" "), color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    }
                    OutlinedButton(onClick = onChooseAudio, shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enviar audio")
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaAccessCard(onOpenMediaAccess: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF26202A))) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("Activa el acceso a música del teléfono", color = androidx.compose.ui.graphics.Color(0xFFFFD6B0), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "Android necesita tu permiso para leer el título, artista y estado del reproductor y enviar órdenes de reproducción.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Button(
                onClick = onOpenMediaAccess,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
            ) {
                Text("Abrir acceso multimedia")
            }
        }
    }
}

@Composable
private fun CurrentTrackCard(state: RemoteUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("MÚSICA DEL TELÉFONO", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(17.dp))
                        .background(Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF343C55), MaterialTheme.colorScheme.surface))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(state.phoneTitle, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(state.phoneArtist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.phoneApp.isNotBlank()) {
                        Text(state.phoneApp, color = androidx.compose.ui.graphics.Color(0xFF8793A6), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(
                    imageVector = if (state.phonePlaying) Icons.Outlined.PlayArrow else Icons.Outlined.MusicNote,
                    contentDescription = if (state.phonePlaying) "Reproduciendo" else "En pausa",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ControlSection(
    state: RemoteUiState,
    controlTv: Boolean,
    onControlTargetChange: (Boolean) -> Unit
) {
    val onTv = state.tvConnected && controlTv
    val source = if (onTv) "SONORA TV" else "TELÉFONO"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "CONTROLES · $source",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (state.tvConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onTv) {
                    OutlinedButton(
                        onClick = { onControlTargetChange(false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("TELÉFONO") }
                    Button(
                        onClick = { onControlTargetChange(true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                    ) { Text("SONORA TV") }
                } else {
                    Button(
                        onClick = { onControlTargetChange(false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                    ) { Text("TELÉFONO") }
                    OutlinedButton(
                        onClick = { onControlTargetChange(true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("SONORA TV") }
                }
            }
        }

        if (onTv) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PadButton(Icons.Outlined.KeyboardArrowUp, "Arriba") {
                    SonoraBridge.sendTvCommand("navigate", org.json.JSONObject().put("direction", "up"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    PadButton(Icons.Outlined.ChevronLeft, "Izquierda") {
                        SonoraBridge.sendTvCommand("navigate", org.json.JSONObject().put("direction", "left"))
                    }
                    PadButton(Icons.Outlined.Check, "Seleccionar", selected = true) {
                        SonoraBridge.sendTvCommand("select")
                    }
                    PadButton(Icons.Outlined.ChevronRight, "Derecha") {
                        SonoraBridge.sendTvCommand("navigate", org.json.JSONObject().put("direction", "right"))
                    }
                }
                PadButton(Icons.Outlined.KeyboardArrowDown, "Abajo") {
                    SonoraBridge.sendTvCommand("navigate", org.json.JSONObject().put("direction", "down"))
                }
            }
        }

        Text(
            when {
                onTv -> "El teléfono controla el reproductor y la biblioteca de Sonora TV."
                state.tvConnected -> "Controlando el reproductor activo del teléfono. Puedes cambiar a Sonora TV arriba."
                else -> "Sin TV conectada, las órdenes se envían al reproductor activo del teléfono."
            },
            color = androidx.compose.ui.graphics.Color(0xFF8793A6),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaybackButton(Icons.Outlined.SkipPrevious, "Anterior") {
                if (onTv) SonoraBridge.sendTvCommand("previous") else SonoraBridge.sendPhoneCommand("previous")
            }
            PlaybackButton(
                if ((if (onTv) state.tvPlaying else state.phonePlaying)) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                if ((if (onTv) state.tvPlaying else state.phonePlaying)) "Pausar" else "Reproducir",
                large = true
            ) {
                if (onTv) SonoraBridge.sendTvCommand("toggle") else SonoraBridge.sendPhoneCommand("toggle")
            }
            PlaybackButton(Icons.Outlined.SkipNext, "Siguiente") {
                if (onTv) SonoraBridge.sendTvCommand("next") else SonoraBridge.sendPhoneCommand("next")
            }
        }

        if (onTv) {
            OutlinedButton(
                onClick = { SonoraBridge.sendTvCommand("open_library") },
                shape = RoundedCornerShape(13.dp)
            ) { Text("Volver a biblioteca del TV") }
        }
    }
}

@Composable
private fun VolumeSection(state: RemoteUiState, controlTv: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.VolumeDown, contentDescription = "Bajar volumen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("VOLUMEN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 2.sp)
            Icon(Icons.Outlined.VolumeUp, contentDescription = "Subir volumen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = state.volume, onValueChange = { SonoraBridge.setVolume(it, controlTv) })
    }
}

@Composable
private fun PadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (selected) 62.dp else 54.dp).clip(CircleShape)
            .background(if (selected) androidx.compose.ui.graphics.Color(0xFF343D50) else androidx.compose.ui.graphics.Color(0xFF1B212D))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFFE3E7EF),
            modifier = Modifier.size(27.dp)
        )
    }
}

@Composable
private fun PlaybackButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    large: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (large) 66.dp else 54.dp).clip(CircleShape)
            .background(if (large) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFF1B212D))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (large) androidx.compose.ui.graphics.Color(0xFF211307) else androidx.compose.ui.graphics.Color(0xFFE3E7EF),
            modifier = Modifier.size(if (large) 31.dp else 25.dp)
        )
    }
}

@Composable
private fun PairingDialog(state: RemoteUiState, onDismiss: () -> Unit, onCopy: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Conectar Sonora TV", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Abre esta dirección en el navegador del televisor o escanea el QR si tu TV tiene cámara.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                if (state.localUrl.isNotBlank()) {
                    QrCodeImage(state.localUrl)
                    Text(state.localUrl, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, lineHeight = 15.sp)
                } else {
                    Text("Conecta el teléfono a una red Wi‑Fi.", color = MaterialTheme.colorScheme.primary)
                }
                Text("CÓDIGO MANUAL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 2.sp)
                Text(state.pin.chunked(3).joinToString(" "), color = MaterialTheme.colorScheme.primary, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("En la página de Sonora TV escribe este PIN para emparejar.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
            }
        },
        confirmButton = { TextButton(onClick = onCopy) { Text("Copiar dirección", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
private fun QrCodeImage(value: String) {
    val bitmap = remember(value) {
        runCatching {
            val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 300, 300)
            Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until 300) {
                    for (y in 0 until 300) {
                        setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Código QR para abrir Sonora TV",
            modifier = Modifier.size(190.dp).clip(RoundedCornerShape(14.dp))
                .background(androidx.compose.ui.graphics.Color.White).padding(8.dp)
        )
    }
}
