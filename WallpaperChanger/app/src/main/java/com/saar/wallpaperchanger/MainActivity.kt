package com.saar.wallpaperchanger;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import eu.long1.spacetablayout.SpaceTabLayout;


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
public class MainActivity extends AppCompatActivity  {

    int myPremmision;

    SpaceTabLayout tabLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);


//        HANDLE PERMISSION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //Should the request be displayed
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        myPremmision);
            }

        }


        createNotificationChannel();

        List<Fragment> fragmentList = new ArrayList<>();

        fragmentList.add(new ServicesFragment());
        fragmentList.add(new DatabaseFragment());
        fragmentList.add(new SearchFragment());


        ViewPager viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.spaceTabLayout);

        tabLayout.initialize(viewPager, getSupportFragmentManager(), fragmentList, null);


    }

    /**
     * Creates the notification channel to be used in the new round name notification
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library

        CharSequence name = getString(R.string.channel_name);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("Set Round Name", name, importance);

        name = getString(R.string.channel_error);
        importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel errorChannel = new NotificationChannel("Error Notifications", name, importance);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        notificationManager.createNotificationChannel(errorChannel);

    }

}