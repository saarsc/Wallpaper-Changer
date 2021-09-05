package com.saar.wallpaperchanger;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String name;
        Bundle input = RemoteInput.getResultsFromIntent(intent);
        if (input != null) {
            Toast.makeText(context, "working", Toast.LENGTH_SHORT).show();
            name = input.getCharSequence("next_round_name").toString();
            SharedPreferences sp = context.getSharedPreferences("ROUND_NAME", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("name", name);
            editor.apply();

            Notification newNotification = new Notification.Builder(context, "Set Round Name")
                    .setSmallIcon(R.drawable.confirmnotificationnewname)
                    .setContentText(name)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(1, newNotification);

        }

    }
}
