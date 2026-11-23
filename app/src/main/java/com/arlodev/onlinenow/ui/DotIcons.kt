package com.arlodev.onlinenow.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.arlodev.onlinenow.AppInfo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads app icons and caches them in two shapes:
 *  - a dot-matrix grid (the default look)
 *  - the original icon as a bitmap (the "Original" icon style)
 */
object DotIcons {
    private const val SIDE = 192
    private const val ADAPTIVE_INSET = 0.19f // adaptive icons keep their art in the centre 61%
    private const val LEGACY_INSET = 0.04f

    private val dots = ConcurrentHashMap<String, DotGrid>()
    private val originals = ConcurrentHashMap<String, ImageBitmap>()

    fun cached(pkg: String): DotGrid? = dots[pkg]

    fun cachedOriginal(pkg: String): ImageBitmap? = originals[pkg]

    fun clear() {
        dots.clear()
        originals.clear()
    }

    suspend fun load(pm: PackageManager, app: AppInfo): DotGrid {
        dots[app.packageName]?.let { return it }
        return withContext(Dispatchers.Default) { compute(pm, app) }
    }

    suspend fun loadOriginal(pm: PackageManager, pkg: String): ImageBitmap? {
        originals[pkg]?.let { return it }
        return withContext(Dispatchers.Default) {
            try {
                val drawable = loadIcon(pm, pkg) ?: return@withContext null
                val bitmap = drawable.toBitmap(160, 160).asImageBitmap()
                originals[pkg] = bitmap
                bitmap
            } catch (_: Throwable) {
                null
            }
        }
    }

    /** Called off the main thread after the app list loads, so the drawer opens fully drawn. */
    fun prewarm(pm: PackageManager, apps: List<AppInfo>) {
        for (app in apps) {
            if (!dots.containsKey(app.packageName)) compute(pm, app)
        }
    }

    private fun compute(pm: PackageManager, app: AppInfo): DotGrid {
        val grid = try {
            render(pm, app)
        } catch (_: Throwable) {
            DotMath.letter(app.label)
        }
        dots[app.packageName] = grid
        return grid
    }

    private fun render(pm: PackageManager, app: AppInfo): DotGrid {
        val icon = loadIcon(pm, app.packageName) ?: return DotMath.letter(app.label)

        val grid: DotGrid = if (icon is AdaptiveIconDrawable) {
            // Best case: the app ships a themed monochrome layer (a clean silhouette).
            val mono = icon.monochrome
            val fromMono = if (mono != null) {
                DotMath.fromPixels(rasterize(mono), SIDE, ADAPTIVE_INSET, true)
            } else {
                null
            }
            if (fromMono != null && fromMono.litCount() >= 8) {
                fromMono
            } else {
                DotMath.fromPixels(rasterize(icon.foreground), SIDE, ADAPTIVE_INSET, false)
            }
        } else {
            DotMath.fromPixels(rasterize(icon), SIDE, LEGACY_INSET, false)
        }

        return if (grid.litCount() >= 6) grid else DotMath.letter(app.label)
    }

    private fun loadIcon(pm: PackageManager, pkg: String): Drawable? = try {
        val component = pm.getLaunchIntentForPackage(pkg)?.component
        if (component != null) pm.getActivityIcon(component) else pm.getApplicationIcon(pkg)
    } catch (_: Exception) {
        null
    }

    private fun rasterize(drawable: Drawable): IntArray {
        val bitmap = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, SIDE, SIDE)
            drawable.draw(canvas)
            val pixels = IntArray(SIDE * SIDE)
            bitmap.getPixels(pixels, 0, SIDE, 0, 0, SIDE, SIDE)
            return pixels
        } finally {
            bitmap.recycle()
        }
    }
}
