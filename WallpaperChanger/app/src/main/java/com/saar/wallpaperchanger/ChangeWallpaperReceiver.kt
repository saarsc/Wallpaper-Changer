package com.saar.wallpaperchanger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.saar.wallpaperchanger.utils.jobUtils

class ChangeWallpaperReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Receiver has received", Toast.LENGTH_SHORT).show()

        jobUtils.scheduleJob(context)
    }
}
