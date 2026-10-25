package com.arlodev.onlinenow

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.arlodev.onlinenow.ui.DefaultAccent
import com.arlodev.onlinenow.ui.ThemeMode
import kotlin.concurrent.thread

data class AppInfo(val label: String, val packageName: String)

class LauncherState(private val appContext: Context) {

    private val sp = appContext.getSharedPreferences("online_now", Context.MODE_PRIVATE)
    private val pm: PackageManager = appContext.packageManager

    companion object {
        const val MAX_FAVORITES = 5
        const val MAX_DOCK = 4
    }

    // ---- data ----
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var favorites by mutableStateOf(readList("favorites"))
        private set

    var dock by mutableStateOf(if (sp.contains("dock")) readList("dock") else defaultDock())
        private set

    var hidden by mutableStateOf<Set<String>>(sp.getStringSet("hidden", emptySet())!!.toSet())
        private set

    // ---- settings ----
    var themeMode by mutableStateOf(
        ThemeMode.entries.firstOrNull { it.name == sp.getString("theme", null) } ?: ThemeMode.Dark
    )
        private set

    var accent by mutableStateOf(Color(sp.getInt("accent", DefaultAccent.toArgb())))
        private set

    var monoIcons by mutableStateOf(sp.getBoolean("mono", true))
        private set

    // ---- UI flags ----
    var drawerOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var menuApp by mutableStateOf<AppInfo?>(null)

    fun closeOverlays() {
        drawerOpen = false
        settingsOpen = false
        menuApp = null
    }

    // ---- loading ----
    fun reloadApps() {
        thread {
            val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0))
                .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
                .filter { it.packageName != appContext.packageName }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            apps = list
        }
    }

    private fun defaultDock(): List<String> {
        val intents = listOf(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:")),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
                .addCategory(Intent.CATEGORY_BROWSABLE),
        )
        return intents
            .mapNotNull {
                pm.resolveActivity(it, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
                    ?.activityInfo?.packageName
            }
            .filter { it != "android" && it != appContext.packageName }
            .distinct()
            .take(MAX_DOCK)
    }

    // ---- launching ----
    fun start(intent: Intent) {
        try {
            appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    fun launch(pkg: String) {
        val intent = pm.getLaunchIntentForPackage(pkg) ?: return
        start(intent)
        drawerOpen = false
    }

    fun openAppInfo(pkg: String) {
        start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)))
    }

    fun uninstall(pkg: String) {
        start(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null)))
    }

    // ---- favorites / dock / hidden ----
    fun toggleFavorite(pkg: String) {
        favorites = when {
            pkg in favorites -> favorites - pkg
            favorites.size < MAX_FAVORITES -> favorites + pkg
            else -> {
                toast("Home is full ($MAX_FAVORITES apps)")
                favorites
            }
        }
        saveList("favorites", favorites)
    }

    fun toggleDock(pkg: String) {
        dock = when {
            pkg in dock -> dock - pkg
            dock.size < MAX_DOCK -> dock + pkg
            else -> {
                toast("Dock is full ($MAX_DOCK apps)")
                dock
            }
        }
        saveList("dock", dock)
    }

    fun toggleHidden(pkg: String) {
        hidden = if (pkg in hidden) hidden - pkg else hidden + pkg
        sp.edit().putStringSet("hidden", hidden).apply()
    }

    // ---- settings setters ----
    fun setTheme(mode: ThemeMode) {
        themeMode = mode
        sp.edit().putString("theme", mode.name).apply()
    }

    fun updateAccent(color: Color) {
        accent = color
        sp.edit().putInt("accent", color.toArgb()).apply()
    }

    fun updateMonoIcons(value: Boolean) {
        monoIcons = value
        sp.edit().putBoolean("mono", value).apply()
    }

    // ---- helpers ----
    private fun readList(key: String): List<String> =
        sp.getString(key, "").orEmpty().split(",").filter { it.isNotBlank() }

    private fun saveList(key: String, list: List<String>) {
        sp.edit().putString(key, list.joinToString(",")).apply()
    }

    private fun toast(text: String) {
        Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
    }
}
