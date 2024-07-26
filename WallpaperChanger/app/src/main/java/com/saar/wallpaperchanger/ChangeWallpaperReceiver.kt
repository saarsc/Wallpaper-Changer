package com.saar.wallpaperchanger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ChangeWallpaperReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Receiver has received", Toast.LENGTH_SHORT).show();

        util.scheduleJob(context);
    }
}
