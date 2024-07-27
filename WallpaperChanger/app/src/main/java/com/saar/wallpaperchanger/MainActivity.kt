package com.saar.wallpaperchanger

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import eu.long1.spacetablayout.SpaceTabLayout

/*TODO
   1) Reset button:
       c. handle UI
   2) Server requests:
       a. Create new note
       b. Update note
   3) Schedule the WP change
       c. Might be forgetting something important in this step
   4) View left WP and the one that have already been used
       a. Simple UI lists
       b.(?) Look into maybe showing all the rounds(If you are really crazy maybe even previous rounds)

*/
class MainActivity : AppCompatActivity() {
    var myPremmision: Int = 0

    var tabLayout: SpaceTabLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)


        //        HANDLE PERMISSION
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Should the request be displayed
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            ) {
                //request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    myPremmision
                )
            }
        }


        createNotificationChannel()

        val fragmentList: MutableList<Fragment> = ArrayList()

        fragmentList.add(ServicesFragment())
        fragmentList.add(DatabaseFragment())
        fragmentList.add(SearchFragment())


        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        tabLayout = findViewById(R.id.spaceTabLayout)
        tabLayout?.initialize(viewPager, supportFragmentManager, fragmentList, null)
    }

    /**
     * Creates the notification channel to be used in the new round name notification
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library

        var name: CharSequence = getString(R.string.channel_name)
        var importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("Set Round Name", name, importance)

        name = getString(R.string.channel_error)
        importance = NotificationManager.IMPORTANCE_DEFAULT
        val errorChannel = NotificationChannel("Error Notifications", name, importance)
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(errorChannel)
    }
}