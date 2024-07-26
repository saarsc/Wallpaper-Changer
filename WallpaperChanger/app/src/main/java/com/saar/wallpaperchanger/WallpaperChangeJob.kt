package com.saar.wallpaperchanger

import android.app.job.JobParameters
import android.app.job.JobService
import android.widget.Toast
import com.saar.wallpaperchanger.util.changeWallpaper
import com.saar.wallpaperchanger.util.scheduleJob

class WallpaperChangeJob : JobService() {
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Toast.makeText(applicationContext, "Job Started", Toast.LENGTH_SHORT).show()
        val context = applicationContext

        val sp = context.getSharedPreferences("currentAlbum", MODE_PRIVATE)
        val nextAlbum = sp.getString("next_album", "")
        if (nextAlbum == "") {
            changeWallpaper(context)
        } else {
            changeWallpaper(context, nextAlbum, true)

            val editor = sp.edit()
            editor.putString("next_album", "")
            editor.apply()
        }

        scheduleJob(context)
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }
}
