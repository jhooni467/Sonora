package com.sonora.remote

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.sonora.remote.connection.SonoraBridge
import com.sonora.remote.ui.RemoteScreen
import com.sonora.remote.ui.SonoraRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        SonoraBridge.initialize(applicationContext)

        setContent {
            val audioPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) SonoraBridge.shareAudioToTv(this, uri)
            }

            SonoraRemoteTheme {
                Surface(color = Color(0xFF090B10)) {
                    RemoteScreen(
                        onChooseAudio = { audioPicker.launch("audio/*") },
                        onOpenMediaAccess = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SonoraBridge.refreshMediaAccess(this)
    }

    override fun onDestroy() {
        if (isFinishing) SonoraBridge.stop()
        super.onDestroy()
    }
}
