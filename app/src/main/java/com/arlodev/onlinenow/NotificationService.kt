package com.arlodev.onlinenow

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow

/** Package names that currently have a visible (non-ongoing) notification. */
object NotificationState {
    val packages = MutableStateFlow<Set<String>>(emptySet())
}

data class MediaInfo(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val packageName: String,
)

/** What is playing right now. Filled by [NotificationService] once notification access is granted. */
object MediaState {
    val info = MutableStateFlow<MediaInfo?>(null)

    @Volatile
    var controller: MediaController? = null

    fun playPause(context: Context) {
        val c = controller
        if (c != null) {
            val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
            if (playing) c.transportControls.pause() else c.transportControls.play()
        } else {
            // nothing known: ask whichever player was used last
            sendKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    private fun sendKey(context: Context, code: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}

class NotificationService : NotificationListenerService() {

    private var sessions: MediaSessionManager? = null
    private var watched: List<MediaController> = emptyList()

    private val component: ComponentName by lazy { ComponentName(this, NotificationService::class.java) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onSessionDestroyed() = publishMedia()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        watch(list.orEmpty())
    }

    override fun onListenerConnected() {
        refresh()
        try {
            val manager = getSystemService(MediaSessionManager::class.java) ?: return
            sessions = manager
            manager.addOnActiveSessionsChangedListener(sessionsListener, component)
            watch(manager.getActiveSessions(component))
        } catch (_: Exception) {
            // no access to media sessions: the music card just stays idle
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    override fun onListenerDisconnected() {
        NotificationState.packages.value = emptySet()
        try {
            sessions?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {
        }
        watch(emptyList())
    }

    private fun watch(controllers: List<MediaController>) {
        for (old in watched) {
            try {
                old.unregisterCallback(controllerCallback)
            } catch (_: Exception) {
            }
        }
        watched = controllers
        for (c in controllers) {
            try {
                c.registerCallback(controllerCallback)
            } catch (_: Exception) {
            }
        }
        publishMedia()
    }

    private fun publishMedia() {
        val current = watched.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: watched.firstOrNull()
        MediaState.controller = current

        val meta = current?.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (current == null || title.isNullOrBlank()) {
            MediaState.info.value = null
            return
        }
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        MediaState.info.value = MediaInfo(
            title = title,
            artist = artist,
            playing = current.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = current.packageName,
        )
    }

    private fun refresh() {
        NotificationState.packages.value = try {
            activeNotifications.orEmpty()
                .filter { !it.isOngoing && it.packageName != packageName }
                .map { it.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
