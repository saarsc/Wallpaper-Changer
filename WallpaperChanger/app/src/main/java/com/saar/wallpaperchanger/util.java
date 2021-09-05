package com.saar.wallpaperchanger;

import android.app.WallpaperManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class util {

    /**
     * Handles all of the service code.:
     * Timing the next run to 22:00 the next day
     * Schedule the service again
     *
     * @param context app context
     */
    public static void scheduleJob(Context context) {


//        Schedule the service
        ComponentName serviceComponent = new ComponentName(context, WallpaperChangeJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(774799256, serviceComponent);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 22);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long timeDelta = c.getTimeInMillis() - System.currentTimeMillis();

        if (timeDelta <= 600000){
            c.add(Calendar.DAY_OF_MONTH,1);
            timeDelta = c.getTimeInMillis() - System.currentTimeMillis();
        }

//        c.add(Calendar.DAY_OF_MONTH, 0);
//        c.add(Calendar.HOUR_OF_DAY, 0);
//        c.add(Calendar.MINUTE, 0);
//        c.add(Calendar.SECOND, 10);

//        start the service every 10-30s CHANGE THIS TO 24h
        builder.setMinimumLatency(timeDelta);
        builder.setOverrideDeadline(timeDelta);

//        Execute no matter what
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setRequiresBatteryNotLow(false);
        builder.setRequiresCharging(false);
        builder.setRequiresDeviceIdle(false);
        builder.setPersisted(true);

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }
    public static void scheduleJobWorker(Context context) {

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 22);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long timeDelta = c.getTimeInMillis() - System.currentTimeMillis();

        if (timeDelta <= 600000){
            c.add(Calendar.DAY_OF_MONTH,1);
            timeDelta = c.getTimeInMillis() - System.currentTimeMillis();
        }

        Constraints constraints = new Constraints.Builder().setRequiresDeviceIdle(false).setRequiresBatteryNotLow(false).setRequiresCharging(false).build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WallpaperChangeWorker.class).setInitialDelay(timeDelta,TimeUnit.MILLISECONDS).setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniqueWork("Wallpaper Changer",ExistingWorkPolicy.REPLACE ,workRequest);
    }
    /**
     * Changes the wallpaper
     *
     * @param context app context
     */
    public static void changeWallpaper(Context context) {
        DbHandler db = new DbHandler(context);

        Photo photo = db.getNewPhoto();

        changeImage(context, photo.getPath());

        int place = db.getPlace(false);
        Date dt = new Date();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        String line = place + ") " + photo.getName() + " - " + LocalDateTime.from(dt.toInstant().atZone(ZoneId.of("Israel"))).plusDays(1).format(formatter);

        sendRequest("update", line,context);
    }

    /**
     * Changes the wallpaper based on the latest image after a restore
     *
     * @param context
     * @param albumName - the latest album that has been used before the restore
     */
    public static void changeWallpaper(Context context, String albumName,Boolean update) {
        DbHandler db = new DbHandler(context);

        Photo photo = db.getAlbumData(albumName);

        changeImage(context, photo.getPath());

        SharedPreferences sp = context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("PATH", photo.getPath());
        editor.putString("NAME", photo.getName());
        editor.putString("ARTIST", photo.getArtist());
        editor.apply();

        if(update){
            int place = db.getPlace(true);
            Date dt = new Date();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            String line = place + ") " + photo.getName() + " - " + LocalDateTime.from(dt.toInstant().atZone(ZoneId.of("Israel"))).plusDays(1).format(formatter);

            sendRequest("update", line,context);
        }
    }

    /**
     * Handles the setting of the image as a wallpaper
     *
     * @param context
     * @param path    - the wallpaper path
     */
    private static void changeImage(Context context, String path) {
        new Thread(() -> {
            WallpaperManager wpm = WallpaperManager.getInstance(context);

//        Converting PNG / JPG to bitmap so it called be set as the wallpaper

            FutureTarget<Bitmap> futureTarget = Glide.with(context).asBitmap().load(path).submit(2280, 2280);
            Bitmap wallpaper = null;
            try {
                wallpaper = futureTarget.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            try {
                wpm.setBitmap(wallpaper);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Glide.with(context).clear(futureTarget);
        }).start();
    }

    /**
     * sends post request to the server
     *
     * @param action - restore -> Restores the DB from gKeep - return the current round data
     *               - new -> to create new round
     *               - update -> to add new album to the list
     * @param album  (AKA extra data) When used with new, it's the next round name. When used with update, it's the new album name
     * @return response -> request result
     */
    public static String sendRequest(String action, String album,Context context) {

        String response = "";
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<String> result = es.submit(() -> {
            String urlAddress = "https://wallpaper.lifemedia.duckdns.org";
            String responseMessage = "ERROR";
            try {
                URL url = new URL(urlAddress);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; utf-8");
                con.setDoOutput(true);
                con.setDoInput(true);

                JSONObject payload = new JSONObject();
                payload.put("action", action);
                payload.put("album", album);
                Log.e("JSON", payload.toString());

                DataOutputStream os = new DataOutputStream(con.getOutputStream());
                os.writeBytes(payload.toString());


                os.flush();
                os.close();

                int responseCode = con.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder builder = new StringBuilder();
                    Reader reader = new InputStreamReader(con.getInputStream(), UTF_8);
                    int data = reader.read();
                    while (data != -1) {
                        builder.append((char) data);
                        data = reader.read();
                    }
                    reader.close();
                    responseMessage = builder.toString();
                }

                con.disconnect();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return responseMessage;
        });

        try {
            response = result.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        es.shutdown();
        if (response.equals("ERROR")){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Set Round Name")
                    .setContentTitle("Didn't update KEEP")
                    .setSmallIcon(R.drawable.notificationicon)
                    .setContentText("Hey dude you need to do it on your own")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(100, builder.build());
        }
        return response;

    }


}


