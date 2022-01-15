package com.jakeguy11.smartnotify;

import android.os.StrictMode;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Channel implements Serializable {

    private String channelName;
    private String channelID;
    private boolean notifyUploads;
    private boolean notifyStreams;
    private boolean filterUploads;
    private boolean filterStreams;
    private List<String> uploadKeywords = new ArrayList<>();
    private List<String> streamKeywords = new ArrayList<>();
    private String pictureURL;
    private String readableID;
    private ChannelType channelType;
    public String latestUploadID;
    public boolean notifiedLive;

    private enum ChannelType {
        CHANNEL,
        C,
        USER
    }

    /**
     * Create an empty Channel object.
     */
    public Channel() {
        // Set the default notification preferences
        this.notifyStreams = true;
        this.notifyUploads = true;
    }

    /**
     * Create a copy of a channel.
     *
     * @param channelToClone the channel to copy
     */
    public Channel(Channel channelToClone) {
        this.channelName = channelToClone.channelName;
        this.channelID = channelToClone.channelID;
        this.notifyUploads = channelToClone.notifyUploads;
        this.notifyStreams = channelToClone.notifyStreams;
        this.filterUploads = channelToClone.filterUploads;
        this.filterStreams = channelToClone.filterStreams;
        this.uploadKeywords = channelToClone.getUploadKeywords();
        this.streamKeywords = channelToClone.getStreamKeywords();
        this.pictureURL = channelToClone.pictureURL;
        this.readableID = channelToClone.readableID;
        this.channelType = channelToClone.channelType;
        this.latestUploadID = channelToClone.latestUploadID;
        this.notifiedLive = channelToClone.notifiedLive;
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
    public boolean setChannelID(String newIdentifier) {
        // Get the readable ID
        String parsedID = parseChannelIdentifier(newIdentifier);
        if (parsedID == null) return false;
        this.readableID = parsedID;

        // Update the picture
        if (!this.updatePicture()) return false;

        // Set the channel ID from the readable ID
        this.channelID = getTrueId(this.readableID);
        if (this.channelID != null) {
            return true;
        } else {
            return false;
        }
    }

    public String getRSSURL() {
        if (this.channelType == ChannelType.CHANNEL) {
            return "https://www.youtube.com/feeds/videos.xml?channel_id=" + this.channelID;
        } else if (this.channelType == ChannelType.USER) {
            return "https://www.youtube.com/feeds/videos.xml?user=" + this.channelID;
        } else {
            return null;
        }
    }

    public String getReadableID() {
        return this.readableID;
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
     * Set whether or not to filter new uploads.
     *
     * @param filter Whether or not to filter upload notifications.
     */
    public void setFilterUploads(boolean filter) {
        this.filterUploads = filter;
    }

    /**
     * Check whether or not to filter new uploads.
     *
     * @return whether or not to filter new uploads.
     */
    public boolean getFilterUploads() {
        return this.filterUploads;
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
     * Set whether or not to filter livestreams.
     *
     * @param filter Whether or not to filter livestreams.
     */
    public void setFilterStreams(boolean filter) {
        this.filterStreams = filter;
    }

    /**
     * Check whether or not to filter livestreams.
     *
     * @return whether or not to filter livestreams.
     */
    public boolean getFilterStreams() {
        return this.filterStreams;
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

    private String getTrueId(String id) {
        Document site = getChannelSite(id);
        if (site == null) return null;
        if (this.channelType != ChannelType.C) return id;

        // Now parse the site for the true id
        String trueURL = site.getElementsByAttributeValue("rel", "canonical").get(1).attr("href");
        String[] splitURL = trueURL.split("/");
        this.channelType = ChannelType.CHANNEL;
        return splitURL[splitURL.length-1];
    }

    /**
     * Update the profile picture of the channel.
     *
     * @return The status of the update. True if success, false if fail.
     */
    public boolean updatePicture() {
        Document site = getChannelSite(this.readableID);
        if (site == null) return false;

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

    private Document getChannelSite(String id) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        Document retSite;
        // Try parsing it with /channel/
        try {
            retSite = Jsoup.parse(Objects.requireNonNull(getSiteContent("https://www.youtube.com/channel/" + id)));
            this.channelType = ChannelType.CHANNEL;
        } catch (Exception e1) {
            // No? Try with /c/
            try {
                retSite = Jsoup.parse(Objects.requireNonNull(getSiteContent("https://www.youtube.com/c/" + id)));
                this.channelType = ChannelType.C;
            } catch (Exception e2) {
                // If not, try legacy with /user/
                try {
                    retSite = Jsoup.parse(Objects.requireNonNull(getSiteContent("https://www.youtube.com/user/" + id)));
                    this.channelType = ChannelType.USER;
                }
                // If none of those worked, the ID is likely not valid.
                catch (Exception e3) {
                    return null;
                }
            }
        }
        return retSite;
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
