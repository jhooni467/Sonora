package com.sonora.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.sonora.remote.ui.RemoteScreen
import com.sonora.remote.ui.SonoraRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SonoraRemoteTheme {
                Surface(color = Color(0xFF090B10)) {
                    RemoteScreen()
                }
            }
        }
    }
}
