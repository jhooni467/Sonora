package com.sonora.tv.ui

import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.ValueCallback
import android.net.Uri
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val TvBackground = androidx.compose.ui.graphics.Color(0xFF090B10)
private val TvPanel = androidx.compose.ui.graphics.Color(0xFF171C26)
private val TvAccent = androidx.compose.ui.graphics.Color(0xFFFFB86B)
private val TvMuted = androidx.compose.ui.graphics.Color(0xFFB9C0CC)

@Composable
fun TvPlayerScreen() {
    var phoneHost by remember { mutableStateOf("") }
    var pairingPin by remember { mutableStateOf("") }
    var playerUrl by remember { mutableStateOf("") }
    var connectionError by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pendingFileCallback?.onReceiveValue(uris.takeIf { it.isNotEmpty() }?.toTypedArray())
        pendingFileCallback = null
    }

    BackHandler(enabled = playerUrl.isNotBlank()) {
        playerUrl = ""
        loading = false
        connectionError = ""
    }

    DisposableEffect(playerUrl) {
        onDispose {
            activeWebView?.let { webView ->
                try {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.removeAllViews()
                    webView.destroy()
                } catch (_: Exception) { }
            }
            activeWebView = null
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TvBackground) {
        if (playerUrl.isBlank()) {
            ConnectionScreen(
                phoneHost = phoneHost,
                onHostChange = { phoneHost = it.filter { char -> char.isDigit() || char == '.' } },
                pairingPin = pairingPin,
                onPinChange = { pairingPin = it.filter(Char::isDigit).take(6) },
                error = connectionError,
                onConnect = {
                    val host = phoneHost.trim()
                    val parts = host.split(".")
                    val validHost = parts.size == 4 && parts.all { part ->
                        val number = part.toIntOrNull()
                        number != null && number in 0..255 && part.isNotEmpty()
                    }
                    if (!validHost) {
                        connectionError = "Introduce la dirección IPv4 que aparece en Sonora Remote."
                    } else if (pairingPin.length != 6) {
                        connectionError = "El PIN debe tener seis cifras."
                    } else {
                        connectionError = ""
                        loading = true
                        playerUrl = "http://$host:8765/?pin=$pairingPin"
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TvBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvPanel)
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "SONORA TV",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp,
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Conectando a $phoneHost",
                            color = TvMuted,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = {
                            playerUrl = ""
                            loading = false
                            connectionError = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvAccent,
                            contentColor = TvBackground
                        )
                    ) {
                        Text("Desconectar")
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                activeWebView = this
                                setBackgroundColor(AndroidColor.rgb(9, 11, 16))
                                isFocusable = true
                                isFocusableInTouchMode = true
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    allowFileAccess = false
                                    allowContentAccess = true
                                    setSupportZoom(false)
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallback: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        if (filePathCallback == null) return false
                                        pendingFileCallback?.onReceiveValue(null)
                                        pendingFileCallback = filePathCallback
                                        audioPicker.launch(arrayOf("audio/*"))
                                        return true
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        loading = false
                                        connectionError = ""
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: android.webkit.WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            loading = false
                                            connectionError = "No se pudo abrir Sonora Remote. Comprueba que el teléfono siga encendido y ambos dispositivos estén en la misma Wi‑Fi."
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val target = request?.url ?: return true
                                        if (target.scheme == "about") return false
                                        return target.scheme != "http" ||
                                            target.host != phoneHost ||
                                            target.port != 8765
                                    }
                                }
                                loadUrl(playerUrl)
                            }
                        }
                    )

                    if (loading) {
                        Card(
                            modifier = Modifier.align(Alignment.Center),
                            colors = CardDefaults.cardColors(containerColor = TvPanel)
                        ) {
                            Text(
                                "Abriendo reproductor…",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                color = TvMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (connectionError.isNotBlank()) {
                    Text(
                        connectionError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvPanel)
                            .padding(12.dp),
                        color = TvAccent,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionScreen(
    phoneHost: String,
    onHostChange: (String) -> Unit,
    pairingPin: String,
    onPinChange: (String) -> Unit,
    error: String,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 56.dp, vertical = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = TvPanel
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Cast,
                        contentDescription = null,
                        tint = TvAccent,
                        modifier = Modifier.padding(13.dp)
                    )
                }
                Text(
                    "SONORA TV",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 25.sp,
                    letterSpacing = 5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Tu música, en pantalla grande.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 41.sp
            )
            Text(
                "Conecta Sonora Remote para abrir tu biblioteca, reproducir archivos del teléfono y controlar la música desde el celular.",
                color = TvMuted,
                fontSize = 17.sp,
                lineHeight = 25.sp
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                androidx.compose.material3.Icon(Icons.Outlined.Wifi, contentDescription = null, tint = TvAccent)
                Text(
                    "Ambos dispositivos deben usar la misma red Wi‑Fi.",
                    color = TvMuted,
                    fontSize = 13.sp
                )
            }
        }

        Card(
            modifier = Modifier.width(440.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = TvPanel)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    "Conectar con el teléfono",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "En Sonora Remote pulsa «Conectar». Introduce aquí la IP y el PIN que aparecen en la pantalla del celular.",
                    color = TvMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                OutlinedTextField(
                    value = phoneHost,
                    onValueChange = onHostChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("IP del teléfono") },
                    placeholder = { Text("192.168.1.20") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    value = pairingPin,
                    onValueChange = onPinChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN de 6 cifras") },
                    placeholder = { Text("123456") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (error.isNotBlank()) {
                    Text(error, color = TvAccent, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TvAccent,
                        contentColor = TvBackground
                    )
                ) {
                    androidx.compose.material3.Icon(Icons.Outlined.Link, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Conectar y abrir reproductor", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
