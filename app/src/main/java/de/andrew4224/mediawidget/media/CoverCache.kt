package de.andrew4224.mediawidget.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Last cover per player, kept (also on disk) until the player sends a new one. */
object CoverCache {

    private const val DIR = "covers"

    private val covers = HashMap<String, Bitmap>()

    @Synchronized
    fun resolve(context: Context, packageName: String, cover: Bitmap?): Bitmap? {
        if (cover == null) return get(context, packageName)
        if (covers[packageName]?.sameAs(cover) != true) {
            write(file(context, packageName), cover)
        }
        covers[packageName] = cover
        return cover
    }

    @Synchronized
    fun get(context: Context, packageName: String): Bitmap? =
        covers[packageName] ?: file(context, packageName)
            .takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.path) }
            ?.also { covers[packageName] = it }

    @Synchronized
    fun forget(context: Context, packageName: String) {
        covers.remove(packageName)
        file(context, packageName).delete()
    }

    private fun write(target: File, cover: Bitmap) {
        target.parentFile?.mkdirs()
        val temp = File(target.path + ".tmp")
        temp.outputStream().use { cover.compress(Bitmap.CompressFormat.PNG, 100, it) }
        temp.renameTo(target)
    }

    private fun file(context: Context, packageName: String) =
        File(File(context.filesDir, DIR), "$packageName.png")
}
