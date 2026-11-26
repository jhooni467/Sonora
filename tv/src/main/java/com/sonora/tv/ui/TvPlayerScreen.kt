package com.sonora.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvPlayerScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF080A0F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF111827), Color(0xFF080A0F), Color(0xFF090B10))
                    )
                )
                .padding(horizontal = 58.dp, vertical = 38.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        "SONORA",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 21.sp,
                        letterSpacing = 5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cast,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        "ESPERANDO CONEXIÓN",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(42.dp)
            ) {
                DefaultArtwork(
                    modifier = Modifier
                        .width(330.dp)
                        .aspectRatio(1f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "REPRODUCTOR MUSICAL",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Esperando música",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "Conecta tu celular para comenzar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 17.sp
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF777F8F))
                        )
                        Text(
                            "SIN REPRODUCCIÓN",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }

                QueuePlaceholder(
                    modifier = Modifier.width(245.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF303746))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TvControl(Icons.Outlined.SkipPrevious, "Anterior")
                    TvControl(Icons.Outlined.PlayArrow, "Reproducir", emphasized = true)
                    TvControl(Icons.Outlined.SkipNext, "Siguiente")
                }
            }
        }
    }
}

@Composable
private fun DefaultArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF39445D),
                        Color(0xFF222B3D),
                        Color(0xFF111722)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Album,
            contentDescription = "Portada musical predeterminada",
            tint = Color(0xFFFFC58B),
            modifier = Modifier.size(126.dp)
        )
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = Color(0xFF20283A),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(25.dp)
                .size(38.dp)
        )
    }
}

@Composable
private fun QueuePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xB0161C27))
            .padding(19.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
            Text(
                "LISTA DE ESPERA",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF343C4A))
        )
        Text(
            "Todavía no hay canciones",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TvControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    emphasized: Boolean = false
) {
    IconButton(
        onClick = {},
        enabled = false,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .size(if (emphasized) 62.dp else 48.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (emphasized) Color(0xFF30394A) else Color.Transparent
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (emphasized) MaterialTheme.colorScheme.primary else Color(0xFF7D8798),
            modifier = Modifier.size(if (emphasized) 31.dp else 25.dp)
        )
    }
}
