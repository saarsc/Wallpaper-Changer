package com.saar.wallpaperchanger.utils

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.saar.wallpaperchanger.WallpaperChangeJob
import com.saar.wallpaperchanger.WallpaperChangeWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object jobUtils {
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
}