package com.saar.wallpaperchanger

import android.app.Activity
import android.app.WallpaperManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import org.json.JSONException
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object util {
    /**
     * Handles all of the service code.:
     * Timing the next run to 22:00 the next day
     * Schedule the service again
     *
     * @param context app context
     */
    @JvmStatic
    fun scheduleJob(context: Context) {
        //        Schedule the service


        val serviceComponent = ComponentName(context, WallpaperChangeJob::class.java)
        val builder = JobInfo.Builder(774799256, serviceComponent)

        val c = Calendar.getInstance()
        c[Calendar.HOUR_OF_DAY] = 22
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        c[Calendar.MILLISECOND] = 0

        var timeDelta = c.timeInMillis - System.currentTimeMillis()

        if (timeDelta <= 600000) {
            c.add(Calendar.DAY_OF_MONTH, 1)
            timeDelta = c.timeInMillis - System.currentTimeMillis()
        }

        //        c.add(Calendar.DAY_OF_MONTH, 0);
//        c.add(Calendar.HOUR_OF_DAY, 0);
//        c.add(Calendar.MINUTE, 0);
//        c.add(Calendar.SECOND, 10);

//        start the service every 10-30s CHANGE THIS TO 24h
        builder.setMinimumLatency(timeDelta)
        builder.setOverrideDeadline(timeDelta)

        //        Execute no matter what
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        builder.setRequiresBatteryNotLow(false)
        builder.setRequiresCharging(false)
        builder.setRequiresDeviceIdle(false)
        builder.setPersisted(true)

        val jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler.schedule(builder.build())
    }

    fun scheduleJobWorker(context: Context?) {
        val c = Calendar.getInstance()
        c[Calendar.HOUR_OF_DAY] = 22
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        c[Calendar.MILLISECOND] = 0

        var timeDelta = c.timeInMillis - System.currentTimeMillis()

        if (timeDelta <= 600000) {
            c.add(Calendar.DAY_OF_MONTH, 1)
            timeDelta = c.timeInMillis - System.currentTimeMillis()
        }

        val constraints =
            Constraints.Builder().setRequiresDeviceIdle(false).setRequiresBatteryNotLow(false)
                .setRequiresCharging(false).setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
        val workRequest = OneTimeWorkRequest.Builder(WallpaperChangeWorker::class.java)
            .setInitialDelay(timeDelta, TimeUnit.MILLISECONDS).setConstraints(constraints).build()
        WorkManager.getInstance(context!!)
            .enqueueUniqueWork("Wallpaper Changer", ExistingWorkPolicy.REPLACE, workRequest)
    }

    fun exportData(activity: Activity, uri: Uri) {
        try {
            val outputStream = activity.contentResolver.openOutputStream(uri)
            val writer = CSVWriter(OutputStreamWriter(outputStream), '|', '"', "\n")

            // Query the database to get the data to export
            val db = DbHandler(activity.applicationContext).readableDatabase
            val cursor = db.rawQuery("SELECT * FROM photos", null)

            // Write the column names to the CSV file
            writer.writeNext(cursor.columnNames)

            // Write the data to the CSV file
            while (cursor.moveToNext()) {
                val row = arrayOfNulls<String>(cursor.columnCount)
                for (i in 0 until cursor.columnCount) {
                    row[i] = cursor.getString(i)
                }
                writer.writeNext(row)
            }

            // Close the CSV writer and the database cursor
            writer.close()
            cursor.close()

            // Show a toast message indicating that the data was exported
            Toast.makeText(activity, "Data exported to $uri", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity, "Error exporting data", Toast.LENGTH_SHORT).show()
        }
    }

    fun importDat(activity: Activity, uri: Uri) {
        try {
            val inputStream = activity.contentResolver.openInputStream(uri)
            val reader = CSVReader(InputStreamReader(inputStream), "|")

            // Delete all existing data from the table
            val dbHandler = DbHandler(activity.applicationContext)
            val db = dbHandler.readableDatabase
            dbHandler.onUpgrade(db, 1, 1)

            // Read the data from the CSV file and insert it into the table
            var row: Array<String>? = null
            while (true) {
                // Read the next line
                val nextRow = reader.readNext()
                if (nextRow == null) break  // Exit loop if no more rows

                // Assign the read row to the variable
                row = nextRow

                // Initialize ContentValues and populate it with row data
                val values = ContentValues()
                for (i in row.indices) {
                    values.put(reader.header[i], row[i])
                }
                db.insert("photos", null, values)
            }


            // Close the CSV reader and the database
            dbHandler.resetArtistsData()
            val c = db.rawQuery(
                "select NAME from photos WHERE USED = 1 ORDER BY USED_ORDER DESC limit 1",
                arrayOf()
            )
            c.moveToFirst()
            val latestAlbum = c.getString(0)
            reader.close()
            db.close()
            changeWallpaper(activity.applicationContext, latestAlbum, false)

            // Show a toast message indicating that the data was imported
            Toast.makeText(activity, "Data imported from $uri", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity, "Error importing data", Toast.LENGTH_SHORT).show()
        }
    }

    fun today(): LocalDateTime {
        val dt = Date()
        return LocalDateTime.from(dt.toInstant().atZone(ZoneId.of("Israel")))
    }

    /**
     * Changes the wallpaper
     *
     * @param context app context
     */
    @JvmStatic
    fun changeWallpaper(context: Context) {
        val db = DbHandler(context)

        val photo = db.newPhoto

        changeImage(context, photo.path)

        val place = db.getPlace(false)
        val dt = Date()

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
        val line = place.toString() + ") " + photo.name + " - " + LocalDateTime.from(
            dt.toInstant().atZone(
                ZoneId.of("Israel")
            )
        ).plusDays(1).format(formatter)

        sendRequest("update", line, context)
    }

    /**
     * Changes the wallpaper based on the latest image after a restore
     *
     * @param context
     * @param albumName - the latest album that has been used before the restore
     */
    @JvmStatic
    fun changeWallpaper(context: Context, albumName: String?, update: Boolean) {
        val db = DbHandler(context)

        val photo = db.getAlbumData(albumName!!)

        changeImage(context, photo!!.path)

        val sp = context.getSharedPreferences("currentAlbum", Context.MODE_PRIVATE)
        val editor = sp.edit()

        editor.putString("PATH", photo.path)
        editor.putString("NAME", photo.name)
        editor.putString("ARTIST", photo.artist)
        editor.apply()

        if (update) {
            val place = db.getPlace(true)
            val dt = Date()

            val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
            val line = place.toString() + ") " + photo.name + " - " + LocalDateTime.from(
                dt.toInstant().atZone(
                    ZoneId.of("Israel")
                )
            ).plusDays(1).format(formatter)

            sendRequest("update", line, context)
        }
    }

    /**
     * Handles the setting of the image as a wallpaper
     *
     * @param context
     * @param path    - the wallpaper path
     */
    private fun changeImage(context: Context, path: String?) {
        Thread {
            val wpm = WallpaperManager.getInstance(context)
            //        Converting PNG / JPG to bitmap so it called be set as the wallpaper
            val futureTarget = Glide.with(context).asBitmap().load(path).submit(3120, 3120)
            var wallpaper: Bitmap? = null
            try {
                wallpaper = futureTarget.get()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            try {
                wpm.setBitmap(wallpaper)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Glide.with(context).clear(futureTarget)
        }.start()
    }

    /**
     * sends post request to the server
     *
     * @param action - restore -> Restores the DB from gKeep - return the current round data
     * - new -> to create new round
     * - update -> to add new album to the list
     * @param album  (AKA extra data) When used with new, it's the next round name. When used with update, it's the new album name
     * @return response -> request result
     */
    fun sendRequest(action: String?, album: String?, context: Context?): String {
        var response = ""
        val es = Executors.newSingleThreadExecutor()
        val result = es.submit<String> {
            val urlAddress = "https://wallpaper.lifemedia.duckdns.org"
            var responseMessage = "ERROR"
            try {
                val url = URL(urlAddress)
                val con = url.openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", "application/json; utf-8")
                con.doOutput = true
                con.doInput = true

                val payload = JSONObject()
                payload.put("action", action)
                payload.put("album", album)
                Log.e("JSON", payload.toString())

                val os = DataOutputStream(con.outputStream)
                os.writeBytes(payload.toString())


                os.flush()
                os.close()

                val responseCode = con.responseCode
                if (responseCode == 200) {
                    val builder = StringBuilder()
                    val reader: Reader = InputStreamReader(con.inputStream, StandardCharsets.UTF_8)
                    var data = reader.read()
                    while (data != -1) {
                        builder.append(data.toChar())
                        data = reader.read()
                    }
                    reader.close()
                    responseMessage = builder.toString()
                }

                con.disconnect()
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            responseMessage
        }

        try {
            response = result.get()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        es.shutdown()
        if (response == "ERROR") {
            val builder = NotificationCompat.Builder(
                context!!, "Set Round Name"
            )
                .setContentTitle("Didn't update KEEP")
                .setSmallIcon(R.drawable.notificationicon)
                .setContentText("Hey dude you need to do it on your own")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            val notificationManager = NotificationManagerCompat.from(context)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    "android.permission.POST_NOTIFICATIONS"
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return ""
            }
            notificationManager.notify(100, builder.build())
        }
        return response
    }
}


