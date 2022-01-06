package com.jakeguy11.smartnotify;

import android.content.Context;
import android.os.StrictMode;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Channel {

    private String channelName;
    private String channelID;
    private boolean notifyUploads;
    private boolean notifyStreams;
    private List<String> uploadKeywords = new ArrayList<>();
    private List<String> streamKeywords = new ArrayList<>();
    private String pictureURL;
    public String latestUploadID;
    public boolean notifiedLive;
    public boolean favourited;

    /**
     * Create an empty Channel object.
     */
    public Channel() {
        // Set the default notification preferences
        this.notifyStreams = true;
        this.notifyUploads = true;
    }

    /**
     * Create a new Channel object and initialize it with the name and ID.
     *
     * @param newChannelName       The name to initialize the channel with.
     * @param newChannelIdentifier The channel ID *or* URL to initialize the channel with.
     */
    public Channel(String newChannelName, String newChannelIdentifier) {
        this.notifyStreams = true;
        this.notifyUploads = true;

        this.channelName = newChannelName;
        this.channelID = parseChannelIdentifier(newChannelIdentifier);
        this.updatePicture();
    }

    /**
     * Change the channel's name. Will not be updated if the new name is null.
     *
     * @param newName The name to update with.
     */
    public void setChannelName(String newName) {
        if (newName == null) return;

        this.channelName = newName;
    }

    /**
     * Get the name assigned to this channel.
     *
     * @return the name of the channel.
     */
    public String getChannelName() {
        return this.channelName;
    }

    /**
     * Change the channel's ID. Will not be updated if the new ID is null or not valid syntactically.
     *
     * @param newIdentifier The ID to update with.
     */
    public void setChannelID(String newIdentifier) {
        String parsedID = parseChannelIdentifier(newIdentifier);

        if (parsedID == null) return;
        this.channelID = parsedID;
        this.updatePicture();
    }

    /**
     * Get the ID assigned to this channel.
     *
     * @return the ID of the channel.
     */
    public String getChannelID() {
        return this.channelID;
    }

    /**
     * Set whether or not to send a notification for new uploads.
     *
     * @param notify Whether or not to notify the user on uploads.
     */
    public void setNotifyUploads(boolean notify) {
        this.notifyUploads = notify;
    }

    /**
     * Check whether or not to send a notification for new uploads.
     *
     * @return whether or not to notify on uploads.
     */
    public boolean getNotifyUploads() {
        return this.notifyUploads;
    }

    /**
     * Set whether or not to send a notification for livestreams.
     *
     * @param notify Whether or not to notify the user on livestreams.
     */
    public void setNotifyStreams(boolean notify) {
        this.notifyStreams = notify;
    }

    /**
     * Check whether or not to send a notification for livestreams.
     *
     * @return whether or not to notify on livestreams.
     */
    public boolean getNotifyStreams() {
        return this.notifyStreams;
    }

    /**
     * Remove all the keywords from the Stream filter.
     */
    public void clearStreamKeywords() {
        this.streamKeywords = new ArrayList<>();
    }

    /**
     * Remove a keyword from the Stream filter.
     *
     * @param keyword The keyword to remove.
     */
    public void removeStreamKeyword(String keyword) {
        if (keyword == null) return;
        this.streamKeywords.remove(keyword);
    }

    /**
     * Add a keyword to the Stream filter.
     *
     * @param keyword The keyword to add.
     */
    public void addStreamKeyword(String keyword) {
        for (String item : this.streamKeywords)
            if (keyword.equals(item)) return;

        this.streamKeywords.add(keyword);
    }

    /**
     * Get a list of all the keywords in the Stream filter.
     *
     * @return a List containing all the keywords in the Stream filter.
     */
    public List<String> getStreamKeywords() {
        return this.streamKeywords;
    }

    /**
     * Remove all the keywords from the Upload filter.
     */
    public void clearUploadKeywords() {
        this.uploadKeywords = new ArrayList<>();
    }

    /**
     * Remove a keyword from the Upload filter.
     *
     * @param keyword The keyword to remove.
     */
    public void removeUploadKeyword(String keyword) {
        if (keyword == null) return;
        this.uploadKeywords.remove(keyword);
    }

    /**
     * Add a keyword to the Upload filter.
     *
     * @param keyword The keyword to add.
     */
    public void addUploadKeyword(String keyword) {
        for (String item : this.uploadKeywords)
            if (keyword.equals(item)) return;

        this.uploadKeywords.add(keyword);
    }

    /**
     * Get a list of all the keywords in the Upload filter.
     *
     * @return a List containing all the keywords in the Upload filter.
     */
    public List<String> getUploadKeywords() {
        return this.uploadKeywords;
    }

    /**
     * Update the profile picture of the channel.
     *
     * @return The status of the update. True if success, false if fail.
     */
    public boolean updatePicture() {
        // Set the thread policy so we can do network calls synchronously
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        Document site;
        // Try parsing it with /channel/
        try {
            site = Jsoup.parse(Objects.requireNonNull(getSiteContent("https://www.youtube.com/channel/" + this.channelID)));
        } catch (Exception e1) {
            // No? Try with /c/
            try {
                site = Jsoup.parse(Objects.requireNonNull(getSiteContent("https://www.youtube.com/c/" + this.channelID)));
            } catch (Exception e2) {
                // If not, try legacy with /user/
                try {
                    site = Jsoup.parse(Objects.requireNonNull(getSiteContent("https://www.youtube.com/user/" + this.channelID)));
                }
                // If none of those worked, the ID is likely not valid.
                catch (Exception e3) {
                    System.out.println("Could not get site content!\n" + e3);
                    return false;
                }
            }
        }

        String foundPictureURL;
        try {
            // Get where we know the image will be. Should return the URL to the image
            foundPictureURL = site.getElementsByAttributeValue("property", "og:image").attr("content");
        } catch (Exception e) {
            // For some reason, it couldn't find the picture
            return false;
        }

        this.pictureURL = foundPictureURL;
        return true;
    }

    /**
     * Get the raw HTML DOM content of a URL. Returns null if the URL is unavailable
     *
     * @param givenURL The URL to fetch.
     * @return A string containing the entire HTML content of the URL.
     */
    private String getSiteContent(String givenURL) {
        URL url;
        try {
            url = new URL(givenURL);
        } catch (MalformedURLException e) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(),
                StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                builder.append(line);
            }
        } catch (IOException e) {
            return null;
        }

        return builder.toString();
    }

    /**
     * Get the URL of the channel's profile picture. Will *not* manually update in order to use less network.
     *
     * @return The saved URL of the channel's profile picture.
     */
    public String getPictureURL() {
        return this.pictureURL;
    }

    /**
     * Turn this channel into a JSON, write it to file. Saved as [pathPrefix]/channelID.json.
     *
     * @param pathPrefix The folder to save into. Must be a *single* folder
     * @param cxt        The application context.
     * @return The status of the write. True if everything was successful, false if it failed.
     */
    public boolean writeJSON(String pathPrefix, Context cxt) {
        // Turn this channel into a JSON string
        Gson gsonMaker = new Gson();
        String formattedChannel = gsonMaker.toJson(this);

        // Write the JSON to file
        // Check if the pathPrefix exists, if not, create it
        File dir = new File(cxt.getFilesDir(), pathPrefix);
        if (!dir.exists()) if (!dir.mkdir()) return false;

        // Write the JSON
        try {
            File jsonFile = new File(dir, this.channelID + ".json");
            FileWriter writer = new FileWriter(jsonFile);
            writer.append(formattedChannel);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Convert a JSON string to an actual Channel object.
     *
     * @param jsonData The JSON string to parse.
     * @return The filled in Channel object.
     */
    public static Channel fromJSON(String jsonData) {
        // Parse the JSON, return it
        Gson jsonParser = new Gson();
        return jsonParser.fromJson(jsonData, Channel.class);
    }

    /**
     * Delete this channel's JSON entry.
     *
     * @param pathPrefix The folder the JSON is saved into. Must be a *single* folder.
     * @param cxt        The application context.
     * @return The status of the deletion. True if successful, false if it failed.
     */
    public boolean deleteChannel(String pathPrefix, Context cxt) {
        // Find the file
        File dir = new File(cxt.getFilesDir(), pathPrefix);
        File jsonFile = new File(dir, this.channelID + ".json");

        // Delete it, return the status
        return jsonFile.delete();
    }

    /**
     * Parse a string in order to get just the channel ID. A URL or plain ID can be passed in.
     *
     * @param identifier The URL *or* channel ID.
     * @return just the channel ID.
     */
    private static String parseChannelIdentifier(String identifier) {
        if (identifier == null) return null;

        // Check if the input string contains a '/' - if it does, it's likely a URL
        if (identifier.contains("/")) {
            String[] splitURL = identifier.split("/");
            String foundID = null;

            // Youtube URLs are always either youtube.com/c/ChannelID or youtube.com/user/ChannelID,
            // but they may have some arguments after them (ie. /videos/), but since the ID always
            // comes after /user/ or /c/, we can parse the URL for that
            for (int i = 0; i < splitURL.length; i++)
                if (splitURL[i].equals("c") || splitURL[i].equals("channel") || splitURL[i].equals("user"))
                    foundID = splitURL[i + 1];

            return foundID;
        } else return identifier;
    }

    /**
     * JSONify the Channel and get it as a String.
     *
     * @return the raw JSON containing the details of the channel.
     */
    @NonNull
    public String toString() {
        Gson gsonMaker = new Gson();
        return gsonMaker.toJson(this);
    }
}
