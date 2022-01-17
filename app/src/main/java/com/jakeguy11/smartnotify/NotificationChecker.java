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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class NotificationChecker extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        for (File file : GenericTools.getAllJSONs(context)) {
            // First, get the current channel
            Channel currentChannel = Channel.fromJSON(GenericTools.getFileString(file));

            // Get the RSS feed
            String rssUrl = currentChannel.getRSSURL();
            JSONObject rssJson = GenericTools.convertXMLtoJSON(GenericTools.getUrlContent(rssUrl));

            try {
                // If the channel's new, initialize it with the latest upload
                if (!currentChannel.isInitialized()) {
                    JSONObject newestVideo = rssJson.getJSONObject("feed")
                            .getJSONArray("entry")
                            .getJSONObject(0);
                    String newestID = newestVideo.getJSONObject("videoId").getString("__text");
                    currentChannel.initialize(newestID);
                } else {
                    // The channel's not new - everything after the newest ID saved to the channel must be notified
                    JSONArray entries = rssJson.getJSONObject("feed").getJSONArray("entry");
                    for (int i = entries.length() - 1; i >= 0; i--) {
                        // Until we find the video id of getLatestUploadID(), do nothing
                        // Once we do, everything after that will be new, and therefore
                        // merit sending a notification. Send the JSONObject of the entry
                        // to another method that will parse it, along with the channel,
                        // and send our notification :)
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not parse json for channel " + currentChannel.getChannelName());
            }
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
