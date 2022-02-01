package com.jakeguy11.smartnotify;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;

import java.io.File;

public class FutureNotification extends BroadcastReceiver {

    Logger logger;
    Context context;

    /**
     * Run whenever a future notification is scheduled
     *
     * @param cxt The context of the app
     * @param intent any extra info needed
     */
    @Override
    public void onReceive(Context cxt, Intent intent) {
        this.context = cxt;
        logger = new Logger("background", this.getClass().getSimpleName(), false, this.context);
        logger.space();
        logger.log("Sending scheduled notification");

        String jsonString = intent.getStringExtra("channel");
        Channel channel = Channel.fromJSON(jsonString);
        String title = intent.getStringExtra("title");
        String url = intent.getStringExtra("url");

        showNotification(channel, title, url);
        logger.write();
    }

    /**
     * Show a notification for a livestream
     * @param channel The channel the notification is for
     * @param videoTitle The title of the video
     * @param videoUrl The URL to the video
     */
    private void showNotification(Channel channel, String videoTitle, String videoUrl) {
        if (channel == null) return;

        // Start by generating our params for the notification
        String title = channel.getChannelName() + " is live:";

        // Get the PFP as a bitmap
        File imageFile = new File(this.context.getFilesDir() + File.separator + channel.getChannelID() + File.separator + channel.getChannelID() + ".png");
        Bitmap icon = BitmapFactory.decodeFile(imageFile.getPath());

        // Create our notification manager
        NotificationManager mNotificationManager = (NotificationManager) this.context.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.context, "default");

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

        try {
            // Send the notification
            mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
            logger.log("Sent notification for channel " + channel.getChannelName(), Logger.LogLevel.INFO);
        } catch (Exception e) { logger.log("Failed to send notification: " + e.getClass().getSimpleName(), Logger.LogLevel.ERROR); }

        logger.write();
    }
}
