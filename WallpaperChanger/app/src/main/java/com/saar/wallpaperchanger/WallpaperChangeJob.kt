package com.saar.wallpaperchanger;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class WallpaperChangeJob extends JobService {


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Toast.makeText(getApplicationContext(), "Job Started", Toast.LENGTH_SHORT).show();
        Context context = getApplicationContext();

        SharedPreferences sp = context.getSharedPreferences("currentAlbum",MODE_PRIVATE);
        String nextAlbum = sp.getString("next_album","");
        if(nextAlbum.equals("")) {
            util.changeWallpaper(context);
        }else{
            util.changeWallpaper(context,nextAlbum,true);

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("next_album","");
            editor.apply();
        }

        util.scheduleJob(context);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
