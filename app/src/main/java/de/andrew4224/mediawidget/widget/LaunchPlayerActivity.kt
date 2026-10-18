package de.andrew4224.mediawidget.widget

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.glance.action.ActionParameters
import de.andrew4224.mediawidget.media.MediaSessions
import de.andrew4224.mediawidget.ui.MainActivity

/**
 * Invisible trampoline started by a widget tap. Starting a real activity first keeps the launch
 * in the foreground, so the player's session PendingIntent is not blocked by background
 * activity start restrictions.
 */
class LaunchPlayerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requested = intent.getStringExtra(PACKAGE.name)
        if (!launchSession(requested) && !launchPackage(requested ?: MediaSessions.SPOTIFY_PACKAGE)) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun launchSession(packageName: String?): Boolean {
        if (packageName == null) return false
        val controller = MediaSessions.activeControllers(this).firstOrNull { it.packageName == packageName }
        val sessionActivity = controller?.sessionActivity ?: return false
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE)
        } else {
            @Suppress("DEPRECATION")
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }
        return options.toBundle()
    }

    companion object {
        private const val TAG = "LaunchPlayerActivity"
        val PACKAGE = ActionParameters.Key<String>("package")
    }
}
