package com.arlodev.onlinenow.ui

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A square grid of dots. Every entry is a size level: 0 = off, 1 = small, 2 = medium, 3 = large. */
class DotGrid(val levels: ByteArray) {
    fun litCount(): Int = levels.count { it > 0 }
}

/**
 * Turns an app icon (as raw ARGB pixels) into a dot-matrix picture.
 * Pure math with no Android classes, so it can be unit tested.
 */
object DotMath {
    const val GRID = 16

    /**
     * @param pixels   side*side straight-alpha ARGB pixels
     * @param inset    fraction of each edge to ignore (adaptive icons keep their art in the middle)
     * @param monochrome true when [pixels] is a themed "monochrome" layer: its alpha is the picture
     */
    fun fromPixels(pixels: IntArray, side: Int, inset: Float, monochrome: Boolean): DotGrid {
        val n = GRID
        val alpha = FloatArray(n * n)
        val lum = FloatArray(n * n)
        val lo = (side * inset).toInt()
        val span = side - 2 * lo

        for (cy in 0 until n) {
            val y0 = lo + cy * span / n
            val y1 = max(y0 + 1, lo + (cy + 1) * span / n).coerceAtMost(side)
            for (cx in 0 until n) {
                val x0 = lo + cx * span / n
                val x1 = max(x0 + 1, lo + (cx + 1) * span / n).coerceAtMost(side)
                var sumA = 0f
                var sumL = 0f
                var count = 0
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        val px = pixels[y * side + x]
                        val a = (px ushr 24) / 255f
                        if (a > 0f) {
                            val r = (px shr 16) and 0xFF
                            val g = (px shr 8) and 0xFF
                            val b = px and 0xFF
                            sumL += a * (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
                        }
                        sumA += a
                        count++
                    }
                }
                val i = cy * n + cx
                alpha[i] = if (count > 0) sumA / count else 0f
                lum[i] = if (sumA > 0f) sumL / sumA else 0f
            }
        }

        val v = FloatArray(n * n)
        if (monochrome) {
            for (i in v.indices) v[i] = alpha[i]
        } else {
            var opaque = 0
            var lumSum = 0f
            for (i in alpha.indices) {
                if (alpha[i] > 0.6f) {
                    opaque++
                    lumSum += lum[i]
                }
            }
            val coverage = opaque.toFloat() / alpha.size
            val meanLum = if (opaque > 0) lumSum / opaque else 0.5f

            if (coverage < 0.72f) {
                // A glyph on a transparent background: its shape is the picture,
                // brighter parts get bigger dots (dark glyphs are flipped so they still show).
                val darkGlyph = meanLum < 0.28f
                for (i in v.indices) {
                    val w = if (darkGlyph) 1f - lum[i] else lum[i]
                    v[i] = alpha[i] * (0.35f + 0.65f * w)
                }
            } else {
                // A full picture: show what stands out from the background colour.
                val opaqueLums = ArrayList<Float>()
                for (i in alpha.indices) if (alpha[i] > 0.9f) opaqueLums.add(lum[i])
                opaqueLums.sort()
                val bg = if (opaqueLums.isEmpty()) 0.5f else opaqueLums[opaqueLums.size / 2]
                for (i in v.indices) v[i] = alpha[i] * min(1f, abs(lum[i] - bg) * 2.6f)

                var lit = 0
                for (x in v) if (x > 0.25f) lit++
                if (lit < alpha.size * 0.06f) {
                    // nothing stands out: just draw the plain shape
                    for (i in v.indices) v[i] = alpha[i]
                }
            }
        }

        var peak = 0f
        for (x in v) if (x > peak) peak = x
        val scale = if (peak > 0f) 1f / max(peak, 0.45f) else 1f

        val levels = ByteArray(n * n)
        for (i in v.indices) {
            val s = min(1f, v[i] * scale)
            val level = when {
                s < 0.14f -> 0
                s < 0.42f -> 1
                s < 0.76f -> 2
                else -> 3
            }
            levels[i] = level.toByte()
        }
        return DotGrid(levels)
    }

    /** Fallback picture: the first letter of the app name, drawn with the dot font. */
    fun letter(label: String): DotGrid {
        val ch = label.trim().firstOrNull { it.isLetterOrDigit() } ?: '?'
        val glyph = DotFont.glyph(ch)
        val w = glyph[0].length
        val offX = (GRID - w * 2) / 2
        val offY = (GRID - DotFont.ROWS * 2) / 2
        val levels = ByteArray(GRID * GRID)
        for (r in 0 until DotFont.ROWS) {
            for (c in 0 until w) {
                if (glyph[r][c] != '1') continue
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        levels[(offY + r * 2 + dy) * GRID + offX + c * 2 + dx] = 3
                    }
                }
            }
        }
        return DotGrid(levels)
    }
}
