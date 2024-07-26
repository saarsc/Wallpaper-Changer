package com.saar.wallpaperchanger

import android.app.Notification
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name: String
        val input = RemoteInput.getResultsFromIntent(intent)
        if (input != null) {
            Toast.makeText(context, "working", Toast.LENGTH_SHORT).show()
            name = input.getCharSequence("next_round_name").toString()
            val sp = context.getSharedPreferences("ROUND_NAME", Context.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("name", name)
            editor.apply()

            val newNotification = Notification.Builder(context, "Set Round Name")
                .setSmallIcon(R.drawable.confirmnotificationnewname)
                .setContentText(name)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(1, newNotification)
        }
    }
}
