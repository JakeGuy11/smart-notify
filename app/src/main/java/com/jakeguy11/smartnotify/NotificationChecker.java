package com.jakeguy11.smartnotify;

import static android.content.Context.NOTIFICATION_SERVICE;

import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationChecker extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        for (File file : getAllJSONs(context)) {
            Channel channelToAdd = Channel.fromJSON(getFileString(file));
        }
    }

    private void showNotification(Context context, Channel channel, String videoTitle, boolean isUpload) {
        if (channel == null) return;

        // Start by generating our params for the notification
        String title = "";
        if (isUpload) title = channel.getChannelName() + " has uploaded a video";
        else title = channel.getChannelName() + " is live!";

        // Get the PFP as a bitmap
        File imageFile = new File(context.getFilesDir() + File.separator + channel.getChannelID() + File.separator + channel.getChannelID() + ".png");
        Bitmap icon = BitmapFactory.decodeFile(imageFile.getPath());

        // Create our notification manager
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "default");

        // Set the notification params
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(videoTitle);
        mBuilder.setLargeIcon(icon);
        mBuilder.setSmallIcon(R.drawable.heart_checked);
        mBuilder.setAutoCancel(true);

        // Do some mandatory android stuff
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel("10001", "NOTIFICATION_CHANNEL_NAME", importance);
            mBuilder.setChannelId("10001");
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        assert mNotificationManager != null;

        // Send the notification
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }

}
