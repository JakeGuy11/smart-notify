package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the main activity as the current view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle("Home");

        // Add a listener for the Add button
        findViewById(R.id.btnAddChannel).setOnClickListener(view -> {
            Intent addChannelIntent = new Intent(getApplicationContext(), AddChannelActivity.class);
            addChannelIntent.putExtra("channels_already_added", channelsAlreadyAdded());
            startActivityForResult(addChannelIntent, 738212183); // This code doesn't matter, it just needs to be unique
        });

        // Get all the data saved and add them to the view
        Channel tempChannel = null;
        for (File file : getAllJSONs()) {
            Channel channelToAdd = Channel.fromJSON(getFileString(file));
            addChannelToView(channelToAdd);
            tempChannel = new Channel(channelToAdd);
        }

        System.out.println("sending notif");

        showNotification(tempChannel, "This is the video title", true);

        System.out.println("sent");

        // Start the periodic service
        Intent periodicIntent = new Intent(this, NotificationChecker.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 1248812527, periodicIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pendingIntent);
    }

    private void showNotification(Channel channel, String videoTitle, boolean isUpload) {
        if(channel == null) return;

        // Start by generating our params for the notification
        String title = "";
        if (isUpload) title = channel.getChannelName() + " has uploaded a video";
        else title = channel.getChannelName() + " is live!";

        // Get the PFP as a bitmap
        File imageFile = new File(this.getFilesDir() + File.separator + channel.getChannelID() + File.separator + channel.getChannelID() + ".png");
        Bitmap icon = BitmapFactory.decodeFile(imageFile.getPath());

        // Create our notification manager
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MainActivity.this, "default");

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

    /**
     * Handle the result of another activity after it is closed.
     *
     * @param requestCode  The request code sent to the activity. Should be unique for each intent.
     * @param resultCode   The code returned by the activity.
     * @param returnedData Any data returned by the activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedData) {
        switch (requestCode) {
            case 738212183: // Returning from adding a channel
                if (resultCode == 1) {
                    // Everything went well, add it to the view
                    Channel returnedChannel = Channel.fromJSON(returnedData.getDataString());
                    System.out.println("Channel returned\n\n" + returnedChannel);

                    // Save the channel
                    if (!saveAndFetchChannelData(returnedChannel))
                        showErrorMessage("Could not write channel to filesystem. Please report this error.");
                    // Add the channel to the view
                    addChannelToView(returnedChannel);
                } else if (resultCode == 0) {
                    // User cancelled it. Do nothing, maybe add a message in the future
                    System.out.println("User cancelled the addition");
                } else {
                    // Some other error - notify the user
                    System.out.println("Something went terribly, terribly wrong. You should not be reading this.");
                }
                break;
            case 6697101: // Returning from editing a channel
                if (resultCode == 1) {
                    // Everything went well - edit the entry
                    Channel returnedChannel = Channel.fromJSON(returnedData.getDataString());
                    String entryToEdit = (String) returnedData.getSerializableExtra("entry_to_edit");

                    // Start by deleting the old data
                    deleteChannelData(entryToEdit);

                    // Go through each entry to see if it's the one we want to edit
                    View viewToFind = null;
                    for (int i = 0; i < ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount(); i++) {
                        View currentView = ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildAt(i);

                        if (((TextView) currentView.findViewById(R.id.channelIdTag)).getText().equals(entryToEdit))
                            viewToFind = currentView;
                    }

                    if (viewToFind == null) return; // Nothing to edit

                    // Copy it over, to make it effectively final
                    View viewToEdit = viewToFind;

                    // Update the params of the entry
                    ((TextView) viewToEdit.findViewById(R.id.channelIdTag)).setText(returnedChannel.getChannelID());
                    ((TextView) viewToEdit.findViewById(R.id.labelChannelName)).setText(returnedChannel.getChannelName());

                    // Update the profile pic
                    ImageView profilePic = viewToEdit.findViewById(R.id.imgChannelPic);
                    profilePic.setImageDrawable(getDrawableFromURL(returnedChannel.getPictureURL()));
                    profilePic.setLayoutParams(new LinearLayout.LayoutParams(128, 128));

                    // Update the edit listener
                    viewToEdit.findViewById(R.id.boxEditButton).setOnClickListener(e -> {
                        Intent editChannelIntent = new Intent(getApplicationContext(), AddChannelActivity.class);
                        editChannelIntent.putExtra("entry_to_edit", returnedChannel.getChannelID());
                        editChannelIntent.putExtra("channel_to_edit", returnedChannel);
                        editChannelIntent.putExtra("channels_already_added", channelsAlreadyAdded());
                        startActivityForResult(editChannelIntent, 6697101); // This code doesn't matter, it just needs to be unique
                    });

                    // Update the delete listener
                    viewToEdit.findViewById(R.id.boxRemoveButton).setOnClickListener(ev -> {
                        // Show a confirmation dialogue
                        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(ev.getContext());
                        confirmBuilder.setTitle("Delete " + returnedChannel.getChannelName() + "?");
                        confirmBuilder.setMessage("Are you sure you want to stop receiving notifications from " + returnedChannel.getChannelName() + "?");
                        confirmBuilder.setCancelable(true);
                        confirmBuilder.setPositiveButton("Yes", (dialogInterface, i) -> {
                            // Now delete the entry
                            ((LinearLayout) viewToEdit.getParent()).removeView(viewToEdit);
                            if (deleteChannelData(returnedChannel.getChannelID()))
                                showErrorMessage(returnedChannel.getChannelName() + " deleted");

                            System.out.println("About to remove");
                            // Re-add the "no channel" message if there are no entries left
                            if (((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount() == 0) {
                                // There are none left
                                System.out.println("none left");
                                View noChannelBox = findViewById(R.id.boxNoChannels);
                                if (noChannelBox != null) noChannelBox.setVisibility(View.VISIBLE);
                            } else
                                showErrorMessage("Entry" + returnedChannel.getChannelName() + " could not be deleted!");
                        });
                        confirmBuilder.setNegativeButton("Cancel", ((dialogInterface, i) -> {
                            // User cancelled it
                            System.out.println("Entry not deleted");
                        }));
                        confirmBuilder.show();
                    });

                    // Check if the ID was changed - if it was, delete the old JSON
                    // Then write the JSON with the updated info
                    if (!entryToEdit.equals(returnedChannel.getChannelID())) {
                        // Delete the old entry
                        deleteChannelData(entryToEdit);
                    }
                    if (!saveAndFetchChannelData(returnedChannel))
                        showErrorMessage("Could not write channel to filesystem. Please report this error.");
                } else if (resultCode == 0) {
                    // User cancelled it. Do nothing
                    System.out.println("User cancelled the edit");
                } else {
                    // Some other error - notify the user
                    System.out.println("Something went terribly, terribly wrong. You should not be reading this.");
                }
                break;
            default:
                System.out.println("Received unknown code: " + requestCode);
                break;
        }
        MainActivity.super.onActivityResult(requestCode, resultCode, returnedData);
    }

    /**
     * Add a channel entry to the home page. Automatically categorizes it as favourite vs. all. Assumes the entry has already been written to system
     *
     * @param channel the Channel object to add.
     */
    private void addChannelToView(Channel channel) {
        System.out.println(channel.getRSSURL());
        // Check if the "no channel" message is still there - if it is, delete it
        View noChannelBox = findViewById(R.id.boxNoChannels);
        if (noChannelBox != null) noChannelBox.setVisibility(View.GONE);

        // Create an inflater so we can customize resources
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Get then customize an entry
        View entryToAdd = (View) inflater.inflate(R.layout.channel_entry, null);
        ImageView profilePic = entryToAdd.findViewById(R.id.imgChannelPic);

        // Set the texts
        ((TextView) entryToAdd.findViewById(R.id.labelChannelName)).setText(channel.getChannelName());
        ((TextView) entryToAdd.findViewById(R.id.channelIdTag)).setText(channel.getChannelID());

        // Set the image
        File imageFile = new File(this.getFilesDir() + File.separator + channel.getChannelID() + File.separator + channel.getChannelID() + ".png");
        profilePic.setImageDrawable(Drawable.createFromPath(imageFile.getPath()));
        profilePic.setLayoutParams(new LinearLayout.LayoutParams(128, 128));

        // Add edit listener
        entryToAdd.findViewById(R.id.boxEditButton).setOnClickListener(ev -> {
            Intent editChannelIntent = new Intent(getApplicationContext(), AddChannelActivity.class);
            editChannelIntent.putExtra("entry_to_edit", channel.getChannelID());
            editChannelIntent.putExtra("channel_to_edit", channel);
            editChannelIntent.putExtra("channels_already_added", channelsAlreadyAdded());
            startActivityForResult(editChannelIntent, 6697101); // This code doesn't matter, it just needs to be unique
        });

        // Add delete listener
        entryToAdd.findViewById(R.id.boxRemoveButton).setOnClickListener(ev -> {
            // Show a confirmation dialogue
            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(ev.getContext());
            confirmBuilder.setTitle("Delete " + channel.getChannelName() + "?");
            confirmBuilder.setMessage("Are you sure you want to stop receiving notifications from " + channel.getChannelName() + "?");
            confirmBuilder.setCancelable(true);
            confirmBuilder.setPositiveButton("Yes", (dialogInterface, i) -> {
                // Now delete the entry
                ((LinearLayout) entryToAdd.getParent()).removeView(entryToAdd);
                if (deleteChannelData(channel.getChannelID()))
                    showErrorMessage(channel.getChannelName() + " deleted");

                System.out.println("About to remove");
                // Re-add the "no channel" message if there are no entries left
                if (((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount() == 0) {
                    // There are none left
                    System.out.println("none left");
                    if (noChannelBox != null) noChannelBox.setVisibility(View.VISIBLE);
                } else
                    showErrorMessage("Entry" + channel.getChannelName() + " could not be deleted!");
            });
            confirmBuilder.setNegativeButton("Cancel", ((dialogInterface, i) -> {
                // User cancelled it
                System.out.println("Entry not deleted");
            }));
            confirmBuilder.show();
        });

        // Add the entry to the screen
        ((LinearLayout) findViewById(R.id.boxChannelsHolder)).addView(entryToAdd);
    }

    private boolean deleteChannelData(String idToDelete) {
        System.out.println("Asked to delete id " + idToDelete);
        File file = new File(this.getFilesDir() + File.separator + idToDelete);
        return deleteFolderRecursive(file);
    }

    private boolean deleteFolderRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteFolderRecursive(child);

        return fileOrDirectory.delete();
    }

    private String[] channelsAlreadyAdded() {
        List<String> idsList = new ArrayList<>();
        for (File dir : getFilesDir().listFiles()) {
            idsList.add(dir.getName());
        }
        return idsList.toArray(new String[0]);
    }

    private File[] getAllJSONs() {
        File dir = getFilesDir();
        List<File> jsonFiles = new ArrayList<>();
        for (File currentDir : dir.listFiles()) {
            File[] jsonFilesFromCurrentDir = currentDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().contains(".json");
                }
            });
            jsonFiles.addAll(Arrays.asList(jsonFilesFromCurrentDir));
        }
        return jsonFiles.toArray(new File[0]);
    }

    private String getFileString(File file) {
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

    private boolean saveAndFetchChannelData(Channel channel) {
        try {
            // Create the file's directories
            File dir = new File(this.getFilesDir() + File.separator + channel.getChannelID());
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
            Drawable resizedPfp = resizeDrawable(pfp);
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

    private void showErrorMessage(String msg) {
        int length = Toast.LENGTH_LONG;
        if (msg.length() <= 30) length = Toast.LENGTH_SHORT;
        Toast.makeText(getApplicationContext(), msg, length).show();
    }

    /**
     * Turn an Image URL into a Drawable.
     *
     * @param url the URL of the image.
     * @return the Drawable containing the image.
     */
    private Drawable getDrawableFromURL(String url) {
        try {
            InputStream urlStream = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(urlStream, null);
        } catch (Exception e) {
            return null;
        }
    }

    private Drawable resizeDrawable(Drawable image) {
        Bitmap b = getRoundedCroppedBitmap(((BitmapDrawable) image).getBitmap());
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 128, 128, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }

    private Bitmap getRoundedCroppedBitmap(Bitmap bitmap) {
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

}