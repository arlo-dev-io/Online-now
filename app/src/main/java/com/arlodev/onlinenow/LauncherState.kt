package com.arlodev.onlinenow

import android.app.role.RoleManager
import android.content.ComponentName
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
import androidx.core.app.NotificationManagerCompat
import com.arlodev.onlinenow.ui.BackdropMode
import com.arlodev.onlinenow.ui.DefaultAccent
import com.arlodev.onlinenow.ui.DotIcons
import com.arlodev.onlinenow.ui.DrawerLayout
import com.arlodev.onlinenow.ui.IconStyle
import com.arlodev.onlinenow.ui.ThemeMode
import kotlin.concurrent.thread

class LauncherState(private val appContext: Context) {

    private val sp = appContext.getSharedPreferences("online_now", Context.MODE_PRIVATE)
    private val pm: PackageManager = appContext.packageManager

    companion object {
        const val MAX_HOME = 8
        const val MAX_PINNED = 8
    }

    // ---- data ----
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    /** Apps on the home screen grid (4 per row). */
    var homeApps by mutableStateOf(loadHomeApps())
        private set

    /** Apps pinned at the top of the drawer. */
    var drawerPinned by mutableStateOf(readList("drawer_favorites"))
        private set

    var hidden by mutableStateOf<Set<String>>(sp.getStringSet("hidden", emptySet())!!.toSet())
        private set

    // ---- settings ----
    var themeMode by mutableStateOf(readEnum("theme", ThemeMode.Dark))
        private set

    var accent by mutableStateOf(Color(sp.getInt("accent", DefaultAccent.toArgb())))
        private set

    var textScale by mutableStateOf(sp.getFloat("text_scale", 1f))
        private set

    var iconStyle by mutableStateOf(readEnum("icon_style", IconStyle.Dots))
        private set

    var drawerLayout by mutableStateOf(readEnum("drawer_layout", DrawerLayout.Grid))
        private set

    var backdrop by mutableStateOf(readEnum("backdrop", BackdropMode.Black))
        private set

    var showPills by mutableStateOf(sp.getBoolean("show_pills", true))
        private set

    var showCalendar by mutableStateOf(sp.getBoolean("show_calendar", true))
        private set

    var showMedia by mutableStateOf(sp.getBoolean("show_media", true))
        private set

    var showPlay by mutableStateOf(sp.getBoolean("show_play", true))
        private set

    var autoKeyboard by mutableStateOf(sp.getBoolean("auto_keyboard", false))
        private set

    // ---- UI flags ----
    var drawerOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var menuApp by mutableStateOf<AppInfo?>(null)

    var notificationAccess by mutableStateOf(false)
        private set

    /** True when Online Now is the phone's home app. */
    var isDefaultLauncher by mutableStateOf(false)
        private set

    /** The first-run / "Setup guide" dialog. */
    var setupOpen by mutableStateOf(false)

    /** Set by MainActivity: shows Android's "make this your home app?" pop-up. */
    var homeRoleRequester: (() -> Unit)? = null

    private var setupChecked = false

    /** How see-through the home screen is: 1 = solid black, lower lets the wallpaper show. */
    val homeAlpha: Float
        get() = when (backdrop) {
            BackdropMode.Black -> 1f
            BackdropMode.Dim -> 0.74f
            BackdropMode.Wallpaper -> 0.38f
        }

    val drawerAlpha: Float
        get() = when (backdrop) {
            BackdropMode.Black -> 1f
            BackdropMode.Dim -> 0.93f
            BackdropMode.Wallpaper -> 0.86f
        }

    val versionName: String by lazy {
        try {
            pm.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0)).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun closeOverlays() {
        drawerOpen = false
        settingsOpen = false
        menuApp = null
    }

    /** Called every time the launcher comes back to the front (the user may have just granted access). */
    fun onResume() {
        notificationAccess = try {
            NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)
        } catch (_: Exception) {
            false
        }
        isDefaultLauncher = try {
            appContext.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } catch (_: Exception) {
            false
        }

        // First start: offer the setup guide once, unless everything is already done.
        if (!setupChecked) {
            setupChecked = true
            if (!sp.getBoolean("setup_seen", false) && !(notificationAccess && isDefaultLauncher)) {
                setupOpen = true
            }
        }
    }

    fun dismissSetup() {
        setupOpen = false
        sp.edit().putBoolean("setup_seen", true).apply()
    }

    // ---- permissions ----
    fun requestDefaultLauncher() {
        val requester = homeRoleRequester
        if (requester != null) requester() else start(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    /** Opens Online Now's own page in the notification-access settings (falls back to the full list). */
    fun openNotificationAccess() {
        val component = ComponentName(appContext, NotificationService::class.java).flattenToString()
        try {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(detail)
        } catch (_: Exception) {
            start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    /** Online Now's own App info page: the three-dot menu there has "Allow restricted settings". */
    fun openOwnAppInfo() = openAppInfo(appContext.packageName)

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
            // draw every icon once in the background so the drawer opens fully drawn
            DotIcons.prewarm(pm, list)
        }
    }

    private fun loadHomeApps(): List<String> {
        if (sp.contains("home_apps")) return readList("home_apps")
        // First start of this version: keep what the old version had (home list + dock),
        // or fall back to phone / messages / camera / browser.
        val old = (readList("favorites") + readList("dock")).distinct()
        val seed = (if (old.isNotEmpty()) old else defaultHome()).take(MAX_HOME)
        saveList("home_apps", seed)
        return seed
    }

    private fun defaultHome(): List<String> {
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

    // ---- home / drawer / hidden ----
    fun toggleHome(pkg: String) {
        homeApps = when {
            pkg in homeApps -> homeApps - pkg
            homeApps.size < MAX_HOME -> homeApps + pkg
            else -> {
                toast("Home is full ($MAX_HOME apps)")
                homeApps
            }
        }
        saveList("home_apps", homeApps)
    }

    /** Moves an app one place earlier (-1) or later (+1) on the home grid. */
    fun moveHome(pkg: String, delta: Int) {
        val from = homeApps.indexOf(pkg)
        val to = from + delta
        if (from < 0 || to !in homeApps.indices) return
        val list = homeApps.toMutableList()
        java.util.Collections.swap(list, from, to)
        homeApps = list
        saveList("home_apps", list)
    }

    fun toggleDrawerPin(pkg: String) {
        drawerPinned = when {
            pkg in drawerPinned -> drawerPinned - pkg
            drawerPinned.size < MAX_PINNED -> drawerPinned + pkg
            else -> {
                toast("Pinned apps are full ($MAX_PINNED apps)")
                drawerPinned
            }
        }
        saveList("drawer_favorites", drawerPinned)
    }

    fun toggleHidden(pkg: String) {
        hidden = if (pkg in hidden) hidden - pkg else hidden + pkg
        sp.edit().putStringSet("hidden", hidden).apply()
    }

    // ---- settings setters ----
    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        sp.edit().putString("theme", mode.name).apply()
    }

    fun updateAccent(color: Color) {
        accent = color
        sp.edit().putInt("accent", color.toArgb()).apply()
    }

    fun changeTextScale(delta: Float) {
        val v = (Math.round((textScale + delta) * 10) / 10f).coerceIn(0.7f, 1.4f)
        textScale = v
        sp.edit().putFloat("text_scale", v).apply()
    }

    fun resetTextScale() {
        textScale = 1f
        sp.edit().putFloat("text_scale", 1f).apply()
    }

    fun updateIconStyle(style: IconStyle) {
        iconStyle = style
        sp.edit().putString("icon_style", style.name).apply()
    }

    fun updateDrawerLayout(layout: DrawerLayout) {
        drawerLayout = layout
        sp.edit().putString("drawer_layout", layout.name).apply()
    }

    fun updateBackdrop(mode: BackdropMode) {
        backdrop = mode
        sp.edit().putString("backdrop", mode.name).apply()
    }

    fun updateShowPills(on: Boolean) {
        showPills = on
        sp.edit().putBoolean("show_pills", on).apply()
    }

    fun updateShowCalendar(on: Boolean) {
        showCalendar = on
        sp.edit().putBoolean("show_calendar", on).apply()
    }

    fun updateShowMedia(on: Boolean) {
        showMedia = on
        sp.edit().putBoolean("show_media", on).apply()
    }

    fun updateShowPlay(on: Boolean) {
        showPlay = on
        sp.edit().putBoolean("show_play", on).apply()
    }

    fun updateAutoKeyboard(on: Boolean) {
        autoKeyboard = on
        sp.edit().putBoolean("auto_keyboard", on).apply()
    }

    // ---- helpers ----
    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T {
        val name = sp.getString(key, null)
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    private fun readList(key: String): List<String> =
        sp.getString(key, "").orEmpty().split(",").filter { it.isNotBlank() }

    private fun saveList(key: String, list: List<String>) {
        sp.edit().putString(key, list.joinToString(",")).apply()
    }

    private fun toast(text: String) {
        Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
    }
}
