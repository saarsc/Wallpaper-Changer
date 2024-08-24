package com.saar.wallpaperchanger.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.saar.wallpaperchanger.R
import org.json.JSONException
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

object apiUtils {
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