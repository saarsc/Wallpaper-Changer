package com.saar.wallpaperchanger;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static android.content.Context.MODE_PRIVATE;

public class WallpaperChangeWorker extends Worker {
    private Context context;
    public WallpaperChangeWorker(@NonNull @org.jetbrains.annotations.NotNull Context context, @NonNull @org.jetbrains.annotations.NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @org.jetbrains.annotations.NotNull
    @Override
    public Result doWork() {
        Toast.makeText(getApplicationContext(), "Job Started", Toast.LENGTH_SHORT).show();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Set Round Name")
                .setContentTitle("Work manager is running")
                .setSmallIcon(R.drawable.notificationicon)
                .setContentText("I'm not the problem my dud")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(101, builder.build());

        SharedPreferences sp = this.context.getSharedPreferences("currentAlbum",MODE_PRIVATE);
        String nextAlbum = sp.getString("next_album","");
        if(nextAlbum.equals("")) {
            util.changeWallpaper(this.context);
        }else{
            util.changeWallpaper(this.context,nextAlbum,true);

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("next_album","");
            editor.apply();
        }
        util.scheduleJobWorker(this.context);
        return Result.success();
    }
}
