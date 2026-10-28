package de.andrew4224.mediawidget.widget

import android.app.Activity
import android.app.ActivityOptions
import android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
import android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
import android.app.PendingIntent
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.BAKLAVA
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Bundle
import android.util.Log
import androidx.glance.action.ActionParameters
import de.andrew4224.mediawidget.AppSettings
import de.andrew4224.mediawidget.media.MediaRepository
import de.andrew4224.mediawidget.media.MediaSessions
import de.andrew4224.mediawidget.media.PendingPlay
import de.andrew4224.mediawidget.ui.MainActivity

/**
 * Invisible trampoline started by a widget tap. Starting a real activity first keeps the launch
 * in the foreground, so the player's session PendingIntent is not blocked by background
 * activity start restrictions.
 */
class LaunchPlayerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(PACKAGE.name) ?: AppSettings.favoritePlayer(this)
        val play = intent.getBooleanExtra(PLAY.name, false)
        if (play && packageName != null) MediaRepository.startPlayback(this)
        when {
            packageName == null -> startActivity(Intent(this, MainActivity::class.java))
            play && playSession(packageName) -> Unit
            launchSession(packageName) -> Unit
            launchPackage(packageName) -> if (play) PendingPlay.request(packageName)
            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun session(packageName: String) =
        MediaSessions.activeControllers(this).firstOrNull { it.packageName == packageName }

    /** The player got its session back since the widget was drawn, so it can simply play. */
    private fun playSession(packageName: String): Boolean {
        val controller = session(packageName) ?: return false
        PendingPlay.request(packageName)
        PendingPlay.startIfReady(listOf(controller))
        return true
    }

    private fun launchSession(packageName: String): Boolean {
        val sessionActivity = session(packageName)?.sessionActivity ?: return false
        return try {
            sessionActivity.send(this, 0, null, null, null, null, backgroundStartOptions())
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "Session activity of $packageName was cancelled", e)
            false
        }
    }

    private fun launchPackage(packageName: String): Boolean {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        startActivity(launch)
        return true
    }

    private fun backgroundStartOptions(): Bundle? {
        if (SDK_INT < UPSIDE_DOWN_CAKE) return null
        val options = ActivityOptions.makeBasic()
        if (SDK_INT >= BAKLAVA) {
            options.setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE)
        } else {
            @Suppress("DEPRECATION")
            options.setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }
        return options.toBundle()
    }

    companion object {
        private const val TAG = "LaunchPlayerActivity"
        val PACKAGE = ActionParameters.Key<String>("package")
        val PLAY = ActionParameters.Key<Boolean>("play")
    }
}
