package com.sonora.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.remote.ui.SonoraBridgePanel

private val previewTracks = listOf(
    "Luz de medianoche",
    "Horizonte",
    "Frecuencia"
)

@Composable
fun RemoteScreen() {
    var connected by remember { mutableStateOf(false) }
    var showConnectionInfo by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var trackIndex by remember { mutableIntStateOf(0) }
    var volume by remember { mutableFloatStateOf(0.55f) }

    if (showConnectionInfo) {
        AlertDialog(
            onDismissRequest = { showConnectionInfo = false },
            title = { Text("Conectar con Sonora TV") },
            text = {
                Text("El emparejamiento entre el celular y el televisor se habilitará en la siguiente etapa.")
            },
            confirmButton = {
                TextButton(onClick = { showConnectionInfo = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "SONORA",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "CONTROL REMOTO",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
            }
            Surface(
                shape = CircleShape,
                color = if (connected) Color(0xFF173C31) else Color(0xFF242936)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tv,
                        contentDescription = null,
                        tint = if (connected) Color(0xFF79D5AE) else Color(0xFFB9C0CC),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (connected) "CONECTADO" else "SIN TV",
                        color = if (connected) Color(0xFF79D5AE) else Color(0xFFB9C0CC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        SonoraBridgePanel()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171C26))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF252D3B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Televisor", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (connected) "Sonora TV" else "Ningún dispositivo vinculado",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Button(onClick = { showConnectionInfo = true }) {
                    Text("Conectar")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(
                "AHORA SUENA",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171C26))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF343C55), Color(0xFF171C26))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = "Portada musical predeterminada",
                            tint = Color(0xFFFFC58B),
                            modifier = Modifier.size(37.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            previewTracks[trackIndex],
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Vista previa de Sonora",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "NAVEGACIÓN",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )
            RemotePad(
                onUp = {},
                onDown = {},
                onLeft = {},
                onRight = {},
                onSelect = {}
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "REPRODUCCIÓN",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaybackButton(Icons.Outlined.SkipPrevious, "Anterior") {
                    trackIndex = (trackIndex - 1 + previewTracks.size) % previewTracks.size
                }
                PlaybackButton(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (isPlaying) "Pausar" else "Reproducir", large = true) {
                    isPlaying = !isPlaying
                }
                PlaybackButton(Icons.Outlined.SkipNext, "Siguiente") {
                    trackIndex = (trackIndex + 1) % previewTracks.size
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Outlined.VolumeDown,
                    contentDescription = "Bajar volumen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "VOLUMEN",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Icon(
                    Icons.Outlined.VolumeUp,
                    contentDescription = "Subir volumen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = volume,
                onValueChange = { volume = it }
            )
        }

        Text(
            "CONTROLES DE VISTA PREVIA · SIN CONEXIÓN A TV",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = Color(0xFF737D8E),
            fontSize = 9.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun RemotePad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onSelect: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton(Icons.Outlined.KeyboardArrowUp, "Arriba", onUp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PadButton(Icons.Outlined.ChevronLeft, "Izquierda", onLeft)
            PadButton(Icons.Outlined.Check, "Seleccionar", onSelect, selected = true)
            PadButton(Icons.Outlined.ChevronRight, "Derecha", onRight)
        }
        PadButton(Icons.Outlined.KeyboardArrowDown, "Abajo", onDown)
    }
}

@Composable
private fun PadButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(if (selected) 62.dp else 54.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFF343D50) else Color(0xFF1B212D))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE3E7EF),
            modifier = Modifier.size(27.dp)
        )
    }
}

@Composable
private fun PlaybackButton(
    icon: ImageVector,
    description: String,
    large: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(if (large) 66.dp else 54.dp)
            .clip(CircleShape)
            .background(if (large) MaterialTheme.colorScheme.primary else Color(0xFF1B212D))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (large) Color(0xFF211307) else Color(0xFFE3E7EF),
            modifier = Modifier.size(if (large) 31.dp else 25.dp)
        )
    }
}
