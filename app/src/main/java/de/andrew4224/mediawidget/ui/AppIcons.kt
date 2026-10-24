package de.andrew4224.mediawidget.ui

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** App icons of the players page, kept in memory while the app is running. */
object AppIcons {

    private const val MAX_ICONS = 64

    private val cache = LruCache<String, ImageBitmap>(MAX_ICONS)

    fun get(context: Context, packageName: String, sizePx: Int): ImageBitmap? {
        val key = "$packageName:$sizePx"
        cache.get(key)?.let { return it }
        return runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx)
                .asImageBitmap()
        }.getOrNull()?.also { cache.put(key, it) }
    }
}
