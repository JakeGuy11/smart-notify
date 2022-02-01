package com.jakeguy11.smartnotify;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationChecker extends BroadcastReceiver {

    Logger logger;

    /**
     * Method run when the notification checker is called
     *
     * @param context The context of the app
     * @param intent any additional information
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Create the logger
        String loggerFile = "background";
        logger = new Logger(loggerFile, this.getClass().getSimpleName(), false, context);

        // Update all the entries
        update(context);

        // Write the logger
        logger.space();
        logger.write();
    }

    /**
     * Update all the entries by fetching the RSS feed.
     *
     * @param context the context of the app
     */
    private void update(Context context) {
        logger.log("Checking channels…", Logger.LogLevel.INFO);

        // Get all the JSONs
        File[] filesToCheck = GenericTools.getAllJSONs(context);
        for (File f : filesToCheck)
            logger.log("Found file " + f.getName(), Logger.LogLevel.VERBOSE);

        // Go through each file
        for (File file : filesToCheck) {
            logger.space();
            // First, get the current channel
            Channel currentChannel = Channel.fromJSON(GenericTools.getFileString(file));
            logger.log("Checking file " + currentChannel.getChannelName() + " with latest video ID " + currentChannel.getLatestUploadID(), Logger.LogLevel.INFO);

            // Get the RSS feed
            String rssUrl = currentChannel.getRSSURL();
            JSONObject rssJson = GenericTools.convertXMLtoJSON(GenericTools.getUrlContent(rssUrl));

            try {
                // Get all the entries (latest 15 videos) as an array
                JSONArray entries = rssJson.getJSONObject("feed").getJSONArray("entry");

                // If the channel's new, initialize it with the latest upload
                if (!currentChannel.isInitialized()) {
                    String idToInit = getVideoInfo(entries, 1)[0];
                    currentChannel.initialize(idToInit);
                    logger.log("Channel was not initialized. Initializing with ID " + idToInit, Logger.LogLevel.VERBOSE);
                } else {
                    // The channel's not new - everything after the newest ID saved to the channel must be notified
                    boolean foundLatestUpdateId = false;
                    for (int i = entries.length() - 1; i >= 0; i--) {
                        // Get the data about the current entry
                        String[] videoInfo = getVideoInfo(entries, i);
                        String videoID = videoInfo[0];
                        String videoTitle = videoInfo[1];

                        logger.log("Found video \"" + videoTitle + "\" with ID " + videoID, Logger.LogLevel.VERBOSE);

                        // Check if the ID has been found yet
                        if (foundLatestUpdateId) {
                            logger.log("Found new video \"" + videoTitle + "\" with ID " + videoID + " at element " + i, Logger.LogLevel.INFO);
                            // This is a new video - check if it's live
                            // Depending on the answer, send the notification
                            String videoUrl = "https://www.youtube.com/watch?v=" + videoID;
                            handleVideo(currentChannel, videoTitle, videoUrl, context);
                            currentChannel.updateLatestVideo(videoID);
                        } else {
                            if (videoID.equals(currentChannel.getLatestUploadID())) {
                                logger.log("Found latest video at element " + i + " with ID " + videoID, Logger.LogLevel.VERBOSE);
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
                logger.log("Saving channel to filesystem...", Logger.LogLevel.INFO);
                GenericTools.saveAndFetchChannelData(context, currentChannel);
                logger.log("Channel successfully saved to filesystem.", Logger.LogLevel.INFO);

            } catch (Exception e) {
                logger.log("Failed to parse channel: " + e.getClass().getSimpleName(), Logger.LogLevel.ERROR);
                e.printStackTrace();
            }
        }
    }

    /**
     * Show a notification for a new video
     *
     * @param channel The channel with a new video
     * @param title The title of the video
     * @param videoUrl The URL of the video
     * @param context The context of the app
     * @throws ParseException If the webpage could not be parsed
     */
    private void handleVideo(Channel channel, String title, String videoUrl, Context context) throws ParseException {
        logger.log("Handling something");
        // Get the site code
        Document page;
        try {
            page = getChannelPage(videoUrl);
        } catch (IOException e) {
            logger.log("Failed to fetch the video site at url " + videoUrl, Logger.LogLevel.ERROR);
            return;
        }

        logger.log("Handling video " + title);

        Elements indicator = page.getElementsByAttributeValue("itemprop", "isLiveBroadcast");

        if (indicator.size() == 0) {
            // It's not live
            logger.log("Indicator is zero - normal video");
            showUploadNotification(channel, title, videoUrl, context);
        } else {
            logger.log("Indicator is >0 - live");
            // It is live - get the time it's supposed to start
            Element timeIndicator = page.getElementsByAttributeValue("itemprop", "startDate").get(0);
            String rawTime = timeIndicator.attr("content"); // in format 2022-01-24T13:00:00+00:00
            String[] plainTimes = rawTime.split("[-T:+]");

            // Format the time
            // SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            // Date date = dateFormatter.parse(plainTimes[0] + " " + plainTimes[1]);
            // long millis = date.getTime();

            Date date = new Date();
            date.UTC(Integer.parseInt(plainTimes[0]),
                    Integer.parseInt(plainTimes[1]),
                    Integer.parseInt(plainTimes[2]),
                    Integer.parseInt(plainTimes[3]),
                    Integer.parseInt(plainTimes[4]),
                    Integer.parseInt(plainTimes[5]));

            long millis = date.getTime();

            System.out.println(millis);
            System.out.println(date);

            // Schedule the notification
            scheduleNotification(channel, title, videoUrl, System.currentTimeMillis() + 20000, context);
        }

    }

    /**
     * Get a youtube channel page as a HTML/DOM document
     *
     * @param url The URL to get
     * @return The page as an HTML/DOM element
     * @throws IOException When the page could not be read.
     */
    private Document getChannelPage(String url) throws IOException {
        // Set the thread policy
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        // Get the page
        return Jsoup.connect(url).get();
    }

    /**
     * Get the infos of a video from its JSON entry
     *
     * @param entries The entry list to get data from
     * @param index The index of the entry to get data from
     * @return an array containing [(video's ID), (video's title)]
     * @throws JSONException When the JSON is invalid
     */
    private String[] getVideoInfo(JSONArray entries, int index) throws JSONException {
        // Get the entry at the wanted index
        JSONObject wantedEntry = entries.getJSONObject(index);

        // Get the video id and title
        String entryID = wantedEntry.getString("yt:videoId");
        String entryTitle = wantedEntry.getString("title");

        // Return the values in an array
        return new String[]{entryID, entryTitle};
    }

    /**
     * Schedule a notification for the future
     *
     * @param channel The channel the notification is for
     * @param videoTitle The title of the video
     * @param videoUrl The URL of the video
     * @param time The time (in system millis) to send the notification
     * @param context the context of the app
     */
    private void scheduleNotification(Channel channel, String videoTitle, String videoUrl, long time, Context context) {
        // Create the notification intent
        Intent futureIntent = new Intent(context, FutureNotification.class);
        futureIntent.putExtra("channel", channel.toString());
        futureIntent.putExtra("title", videoTitle);
        futureIntent.putExtra("url", videoUrl);

        // Schedule the notification
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 1248812527, futureIntent, 0);
        AlarmManager futureNotifManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        futureNotifManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

    /**
     * Show a notification for an upload
     *
     * @param channel The channel the video's from
     * @param videoTitle The title of the video
     * @param videoUrl The URL of the video
     * @param context The context of the app
     */
    private void showUploadNotification(Channel channel, String videoTitle, String videoUrl, Context context) {
        if (channel == null) return;
        logger.log("Sending notification for channel " + channel.getChannelName() + " for video " + videoUrl);

        // Start by generating our params for the notification
        String title = channel.getChannelName() + " has uploaded a video";

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

        try {
            // Send the notification
            mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
            logger.log("Sent notification for channel " + channel.getChannelName(), Logger.LogLevel.INFO);
        } catch (Exception e) { logger.log("Failed to send notification: " + e.getClass().getSimpleName(), Logger.LogLevel.ERROR); }

        logger.write();
    }
}