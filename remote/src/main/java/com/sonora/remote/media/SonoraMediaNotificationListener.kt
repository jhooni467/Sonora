package com.sonora.remote.media

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sonora.remote.connection.SonoraBridge

class SonoraMediaNotificationListener : NotificationListenerService() {
    private var sessionManager: MediaSessionManager? = null

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
            SonoraBridge.setPhoneController(null)
        }
    }

    override fun onListenerDisconnected() {
        try { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) { }
        sessionManager = null
        SonoraBridge.setPhoneController(null)
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
            SonoraBridge.setPhoneController(null)
        } catch (_: Exception) {
            SonoraBridge.setPhoneController(null)
        }
    }

    private fun updateController(controllers: List<MediaController>?) {
        val list = controllers.orEmpty()
        val active = list.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: list.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED
        } ?: list.firstOrNull()
        SonoraBridge.setPhoneController(active)
    }
}
