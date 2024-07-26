package com.saar.wallpaperchanger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ChangeWallpaperReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Receiver has received", Toast.LENGTH_SHORT).show()

        util.scheduleJob(context)
    }
}
