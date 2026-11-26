package com.sonora.remote

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            val preferences = remember { getSharedPreferences("sonora-settings", MODE_PRIVATE) }
            var selectedTheme by remember {
                mutableStateOf(preferences.getString("theme", "original") ?: "original")
            }
            var lyricsAnimation by remember {
                mutableStateOf(preferences.getString("lyrics_animation", "actual") ?: "actual")
            }
            val audioPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) SonoraBridge.shareAudioToTv(this, uri)
            }

            SonoraRemoteTheme(themeId = selectedTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    RemoteScreen(
                        onChooseAudio = { audioPicker.launch("audio/*") },
                        onOpenMediaAccess = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        selectedTheme = selectedTheme,
                        onThemeChange = {
                            selectedTheme = it
                            preferences.edit().putString("theme", it).apply()
                        },
                        lyricsAnimation = lyricsAnimation,
                        onLyricsAnimationChange = {
                            lyricsAnimation = it
                            preferences.edit().putString("lyrics_animation", it).apply()
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
