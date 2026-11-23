package com.arlodev.onlinenow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.arlodev.onlinenow.ui.DotIcons
import com.arlodev.onlinenow.ui.LauncherRoot

class MainActivity : ComponentActivity() {

    private lateinit var state: LauncherState

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            DotIcons.clear()
            state.reloadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // no grey scrim behind the 3-button navigation bar
        window.isNavigationBarContrastEnforced = false

        state = LauncherState(applicationContext)
        state.reloadApps()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        setContent { LauncherRoot(state) }
    }

    override fun onResume() {
        super.onResume()
        state.onResume()
    }

    // Pressing Home while the launcher is already open closes drawer/settings/menu.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            state.closeOverlays()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }
}
