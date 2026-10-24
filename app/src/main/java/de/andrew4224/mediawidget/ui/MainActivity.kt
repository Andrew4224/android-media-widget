package de.andrew4224.mediawidget.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit

/** Hosts [SettingsFragment] and its [PlayersFragment] subpage. */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, SettingsFragment())
            }
        }
    }
}
