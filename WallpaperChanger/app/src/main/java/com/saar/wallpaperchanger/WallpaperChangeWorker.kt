package com.saar.wallpaperchanger

import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.saar.wallpaperchanger.util.changeWallpaper

class WallpaperChangeWorker(private val context: Context, workerParams: WorkerParameters) : Worker(
    context, workerParams
) {
    override fun doWork(): Result {
        Toast.makeText(applicationContext, "Job Started", Toast.LENGTH_SHORT).show()
        val builder = NotificationCompat.Builder(
            context, "Set Round Name"
        )
            .setContentTitle("Work manager is running")
            .setSmallIcon(R.drawable.notificationicon)
            .setContentText("I'm not the problem my dud")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(101, builder.build())

        val sp = context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE)
        val nextAlbum = sp.getString("next_album", "")
        if (nextAlbum == "") {
            changeWallpaper(this.context)
        } else {
            changeWallpaper(this.context, nextAlbum, true)

            val editor = sp.edit()
            editor.putString("next_album", "")
            editor.apply()
        }

        return Result.success()
    }
}
