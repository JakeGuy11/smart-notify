package com.jakeguy11.smartnotify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.StrictMode;
import android.widget.Toast;

import org.json.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class GenericTools {

    /**
     * Convert an XML (ex. rss feed) to a JSON
     *
     * @param xmlContent The XML content as a string
     * @return the JSONified version of the XML
     */
    public static JSONObject convertXMLtoJSON(String xmlContent) {
        try {
            return XML.toJSONObject(xmlContent);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Get all the saved JSONs, anywhere in this app's storage
     * @param context The context of the app
     * @return An array of the JSON files
     */
    public static File[] getAllJSONs(Context context) {
        File dir = context.getFilesDir();
        List<File> jsonFiles = new ArrayList<>();
        for (File currentDir : Objects.requireNonNull(dir.listFiles())) {

            // Skip if it's not a directory
            if (!currentDir.isDirectory()) continue;

            // If the directory is the log dir, skip
            if (currentDir.getName().equals("logs")) continue;

            // Go through each dir
            File[] jsonFilesFromCurrentDir = currentDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().contains(".json");
                }
            });
            jsonFiles.addAll(Arrays.asList(Objects.requireNonNull(jsonFilesFromCurrentDir)));
        }
        return jsonFiles.toArray(new File[0]);
    }

    /**
     * Turn an Image URL into a Drawable.
     *
     * @param url the URL of the image.
     * @return the Drawable containing the image.
     */
    public static Drawable getDrawableFromURL(String url) {
        try {
            InputStream urlStream = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(urlStream, null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the content of a file as a string
     *
     * @param file The file to get the content of
     * @return the content of the file
     */
    public static String getFileString(File file) {
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            return null;
        }

        return text.toString();
    }

    /**
     * Turn a bitmap into a rounded image
     *
     * @param bitmap The raw bitmap
     * @return the rounded bitmap
     */
    public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap) {
        int widthLight = bitmap.getWidth();
        int heightLight = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paintColor = new Paint();
        paintColor.setFlags(Paint.ANTI_ALIAS_FLAG);

        RectF rectF = new RectF(new Rect(0, 0, widthLight, heightLight));

        canvas.drawRoundRect(rectF, widthLight / 2, heightLight / 2, paintColor);

        Paint paintImage = new Paint();
        paintImage.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, 0, 0, paintImage);

        return output;
    }

    /**
     * Get the content of an internet site as a string
     *
     * @param urlString The URL to fetch
     * @return the content of the website
     */
    public static String getUrlContent(String urlString) {
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

            URL url = new URL(urlString);
            URLConnection con = url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null)
                content.append(line).append("\n");

            return content.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resize a drawable image to the default 128x128
     *
     * @param context The context of the app
     * @param image the image to resize
     * @return the resized image
     */
    public static Drawable resizeDrawable(Context context, Drawable image) {
        Bitmap b = getRoundedCroppedBitmap(((BitmapDrawable) image).getBitmap());
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 128, 128, false);
        return new BitmapDrawable(context.getResources(), bitmapResized);
    }

    /**
     * Update the data of a channel from remote, then write it to the filesystem
     *
     * @param context The context of the app
     * @param channel The channel to update and save
     * @return whether it was a success or failure
     */
    public static boolean saveAndFetchChannelData(Context context, Channel channel) {
        try {
            // Create the file's directories
            File dir = new File(context.getFilesDir() + File.separator + channel.getChannelID());
            if (!dir.exists()) {
                // Folder doesn't exist - create it
                dir.mkdir();
            }

            // Create the JSON
            File jsonFile = new File(dir, channel.getChannelID() + ".json");
            FileOutputStream outStream = new FileOutputStream(jsonFile);
            OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream);
            outStreamWriter.write(channel.toString());
            outStreamWriter.close();

            // Create the image
            Drawable pfp = getDrawableFromURL(channel.getPictureURL());
            Drawable resizedPfp = resizeDrawable(context, pfp);
            Bitmap imageToWrite = ((BitmapDrawable) resizedPfp).getBitmap();
            File imageFile = new File(dir, channel.getChannelID() + ".png");
            outStream = new FileOutputStream(imageFile);
            imageToWrite.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

            return true;
        }
        // If there are any errors, return false
        catch (IOException e) {
            return false;
        }
    }

    /**
     * Show a Toast message (grey popup at the bottom of the screen)
     * @param context The context of the app
     * @param msg the message to send
     */
    public static void showErrorMessage(Context context, String msg) {
        int length = Toast.LENGTH_LONG;
        if (msg.length() <= 30) length = Toast.LENGTH_SHORT;
        Toast.makeText(context.getApplicationContext(), msg, length).show();
    }

}