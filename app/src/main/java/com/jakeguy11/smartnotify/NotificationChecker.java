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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class NotificationChecker extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        showBlankNotif(context);
        for (File file : GenericTools.getAllJSONs(context)) {
            // First, get the current channel
            Channel currentChannel = Channel.fromJSON(GenericTools.getFileString(file));
            System.out.println("Checking channel " + currentChannel.getChannelName());
            System.out.println("Latest channel ID is " + currentChannel.getLatestUploadID());

            // Get the RSS feed
            String rssUrl = currentChannel.getRSSURL();
            JSONObject rssJson = GenericTools.convertXMLtoJSON(GenericTools.getUrlContent(rssUrl));

            try {
                // Get all the entries (latest 15 videos) as an array
                JSONArray entries = rssJson.getJSONObject("feed").getJSONArray("entry");

                // If the channel's new, initialize it with the latest upload
                if (!currentChannel.isInitialized()) {
                    String idToInit = getVideoInfo(entries, 0)[0];
                    currentChannel.initialize(idToInit);
                    System.out.println("Initialized with id " + idToInit);
                } else {
                    // The channel's not new - everything after the newest ID saved to the channel must be notified
                    boolean foundLatestUpdateId = false;
                    for (int i = entries.length() - 1; i >= 0; i--) {
                        // Until we find the video id of getLatestUploadID(), do nothing
                        // Once we do, everything after that will be new, and therefore
                        // merit sending a notification. Send the JSONObject of the entry
                        // to another method that will parse it, along with the channel,
                        // and send our notification :)

                        // Get the data about the current entry
                        String[] videoInfo = getVideoInfo(entries, i);
                        String videoID = videoInfo[0];
                        String videoTitle = videoInfo[1];

                        System.out.println("Checking video " + videoID);

                        // Check if the ID has been found yet
                        if (foundLatestUpdateId) {
                            System.out.println("New video at id " + videoID);
                            // This is a new video - check if it's live
                            // Depending on the answer, send the notification
                            String videoUrl = "https://www.youtube.com/watch?v=" + videoID;
                            showNotification(context, currentChannel, videoTitle, videoUrl, videoIsLivestream(videoUrl));
                            currentChannel.updateLatestVideo(videoID);
                        } else {
                            if (videoID.equals(currentChannel.getLatestUploadID())) {
                                System.out.println("Found latest id at index " + i);
                                foundLatestUpdateId = true;
                            }
                        }

                        // If we're on the latest video AND the latest ID hasn't been found yet, something went wrong so invalidate the latest entry
                        if (!foundLatestUpdateId && i == 0) {
                            System.out.println("Something went wrong - latestVideoID invalid");
                            currentChannel.invalidateActivity();
                        }
                    }
                }

                // Re-write the channel to the FS
                GenericTools.saveAndFetchChannelData(context, currentChannel);

            } catch (Exception e) {
                System.out.println("Could not parse json for channel " + currentChannel.getChannelName());
            }
        }
    }

    private String[] getVideoInfo(JSONArray entries, int index) throws JSONException {
        // Get the entry at the wanted index
        JSONObject wantedEntry = entries.getJSONObject(index);

        // Get the video id and title
        String entryID = wantedEntry.getString("yt:videoId");
        String entryTitle = wantedEntry.getString("title");

        // Return the values in an array
        return new String[]{entryID, entryTitle};
    }

    private boolean videoIsLivestream(String videoUrl) {
        return false;
    }

    private void showNotification(Context context, Channel channel, String videoTitle, String videoUrl, boolean isLivestream) {
        if (channel == null) return;

        // Start by generating our params for the notification
        String title = "";
        if (isLivestream) title = channel.getChannelName() + " is live!";
        else title = channel.getChannelName() + " has uploaded a video";

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

    private void showBlankNotif(Context context) {
        // Create our notification manager
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "default");

        // Set the notification params
        mBuilder.setContentTitle("I am sending a notif");
        mBuilder.setContentText(Long.toString(System.currentTimeMillis()));
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
