package com.arlodev.onlinenow.ui

import com.arlodev.onlinenow.AppInfo
import java.text.Normalizer

/** One row of the app drawer list. */
sealed interface DrawerItem {
    val key: String

    data class Header(val text: String) : DrawerItem {
        override val key: String get() = "h_$text"
    }

    /** A row holding 1 app (list layout) or up to [DrawerModelBuilder] columns (grid layout). */
    data class Apps(val apps: List<AppInfo>) : DrawerItem {
        override val key: String get() = "a_" + apps.first().packageName
    }
}

class DrawerModel(
    val items: List<DrawerItem>,
    /** first list index of each letter, used by the A-Z rail */
    val letterIndex: Map<Char, Int>,
) {
    val firstApp: AppInfo?
        get() = items.firstNotNullOfOrNull { (it as? DrawerItem.Apps)?.apps?.firstOrNull() }
}

object DrawerModelBuilder {

    /** 'A'..'Z' for the first letter of the name (accents ignored), '#' for everything else. */
    fun letterOf(label: String): Char {
        val folded = Normalizer.normalize(label.trim(), Normalizer.Form.NFD)
        val c = folded.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: return '#'
        return if (c in 'A'..'Z') c else '#'
    }

    /**
     * @param columns 1 = list with letter headers, 2+ = grid of that many columns
     * @param pinned  shown first (only when the search box is empty)
     */
    fun build(apps: List<AppInfo>, pinned: List<AppInfo>, query: String, columns: Int): DrawerModel {
        val cols = columns.coerceAtLeast(1)
        val q = query.trim().lowercase()
        val items = ArrayList<DrawerItem>()
        val letters = LinkedHashMap<Char, Int>()

        if (q.isNotEmpty()) {
            val matches = apps
                .filter { it.label.lowercase().contains(q) }
                .sortedWith(compareBy<AppInfo>({ !it.label.lowercase().startsWith(q) }, { it.label.lowercase() }))
            for (row in matches.chunked(cols)) items.add(DrawerItem.Apps(row))
            return DrawerModel(items, emptyMap())
        }

        val pinnedKeys = pinned.map { it.packageName }.toSet()
        if (pinned.isNotEmpty()) {
            items.add(DrawerItem.Header("PINNED"))
            for (row in pinned.chunked(cols)) items.add(DrawerItem.Apps(row))
            items.add(DrawerItem.Header("ALL APPS"))
        }

        val rest = apps
            .filter { it.packageName !in pinnedKeys }
            .sortedWith(compareBy<AppInfo>({ letterOf(it.label) }, { it.label.lowercase() }))

        if (cols == 1) {
            var current: Char? = null
            for (app in rest) {
                val l = letterOf(app.label)
                if (l != current) {
                    current = l
                    letters.putIfAbsent(l, items.size)
                    items.add(DrawerItem.Header(l.toString()))
                }
                items.add(DrawerItem.Apps(listOf(app)))
            }
        } else {
            for (row in rest.chunked(cols)) {
                val rowIndex = items.size
                for (app in row) letters.putIfAbsent(letterOf(app.label), rowIndex)
                items.add(DrawerItem.Apps(row))
            }
        }
        return DrawerModel(items, letters)
    }
}
