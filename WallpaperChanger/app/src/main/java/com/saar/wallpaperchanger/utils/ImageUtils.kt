package com.saar.wallpaperchanger.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.saar.wallpaperchanger.DbHandler
import com.saar.wallpaperchanger.utils.apiUtils.sendRequest
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.ExecutionException

object imageUtils {
    /**
     * Changes the wallpaper
     *
     * @param context app context
     */
    @JvmStatic
    fun changeWallpaper(context: Context) {
        val db = DbHandler(context)

        val photo = db.newPhoto

        changeImage(context, photo.path)

        val place = db.getPlace(false)
        val dt = Date()

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
        val line = place.toString() + ") " + photo.name + " - " + LocalDateTime.from(
            dt.toInstant().atZone(
                ZoneId.of("Israel")
            )
        ).plusDays(1).format(formatter)

        sendRequest("update", line, context)
    }

    /**
     * Changes the wallpaper based on the latest image after a restore
     *
     * @param context
     * @param albumName - the latest album that has been used before the restore
     */
    @JvmStatic
    fun changeWallpaper(context: Context, albumName: String?, update: Boolean) {
        val db = DbHandler(context)

        val photo = db.getAlbumData(albumName!!)

        changeImage(context, photo!!.path)

        val sp = context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE)
        val editor = sp.edit()

        editor.putString("PATH", photo.path)
        editor.putString("NAME", photo.name)
        editor.putString("ARTIST", photo.artist)
        editor.apply()

        if (update) {
            val place = db.getPlace(true)
            val dt = Date()

            val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
            val line = place.toString() + ") " + photo.name + " - " + LocalDateTime.from(
                dt.toInstant().atZone(
                    ZoneId.of("Israel")
                )
            ).plusDays(1).format(formatter)

            sendRequest("update", line, context)
        }
    }

    /**
     * Handles the setting of the image as a wallpaper
     *
     * @param context
     * @param path    - the wallpaper path
     */
    private fun changeImage(context: Context, path: String?) {
        Thread {
            val wpm = WallpaperManager.getInstance(context)
            //        Converting PNG / JPG to bitmap so it called be set as the wallpaper
            val futureTarget = Glide.with(context).asBitmap().load(path).submit(3120, 3120)
            var wallpaper: Bitmap? = null
            try {
                wallpaper = futureTarget.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            try {
                wpm.setBitmap(wallpaper)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Glide.with(context).clear(futureTarget)
        }.start()
    }
}