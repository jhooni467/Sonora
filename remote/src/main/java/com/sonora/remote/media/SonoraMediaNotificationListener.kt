package com.sonora.remote.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sonora.remote.connection.SonoraBridge

class SonoraMediaNotificationListener : NotificationListenerService() {
    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            activeController?.let(SonoraBridge::setPhoneController)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            activeController?.let(SonoraBridge::setPhoneController)
        }

        override fun onSessionDestroyed() {
            activeController = null
            refreshSessions()
        }
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateController(controllers)
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(MediaSessionManager::class.java)
        try {
            sessionManager?.addOnActiveSessionsChangedListener(
                sessionsListener,
                ComponentName(this, SonoraMediaNotificationListener::class.java)
            )
            refreshSessions()
        } catch (_: SecurityException) {
            clearController()
        }
    }

    override fun onListenerDisconnected() {
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) { }
        clearController()
        sessionManager = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshSessions()
    }

    private fun refreshSessions() {
        try {
            val component = ComponentName(this, SonoraMediaNotificationListener::class.java)
            updateController(sessionManager?.getActiveSessions(component))
        } catch (_: SecurityException) {
            clearController()
        } catch (_: Exception) {
            clearController()
        }
    }

    private fun updateController(controllers: List<MediaController>?) {
        val list = controllers.orEmpty()
        val selected = list.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: list.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: list.firstOrNull()

        val current = activeController
        if (current?.sessionToken == selected?.sessionToken && current != null) {
            SonoraBridge.setPhoneController(current)
            return
        }

        try {
            current?.unregisterCallback(controllerCallback)
        } catch (_: Exception) { }

        activeController = selected
        try {
            selected?.registerCallback(controllerCallback, Handler(Looper.getMainLooper()))
        } catch (_: Exception) { }
        SonoraBridge.setPhoneController(selected)
    }

    private fun clearController() {
        try {
            activeController?.unregisterCallback(controllerCallback)
        } catch (_: Exception) { }
        activeController = null
        SonoraBridge.setPhoneController(null)
    }
}
