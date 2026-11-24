package com.arlodev.onlinenow

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

    // Result of Android's "make this your home app?" pop-up: just refresh what we show.
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        state.onResume()
    }

    private fun requestHomeRole() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            roleLauncher.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            state.start(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // no grey scrim behind the 3-button navigation bar
        window.isNavigationBarContrastEnforced = false

        state = LauncherState(applicationContext)
        state.reloadApps()
        state.homeRoleRequester = { requestHomeRole() }

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
