package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Logger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the main activity as the current view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the logger
        String loggerFileName = String.valueOf(System.currentTimeMillis());
        logger = new Logger(loggerFileName, this.getClass().getSimpleName(), getApplicationContext());
        logger.log("Starting logger...", Logger.LogLevel.INFO);

        // Configure the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Home");
            logger.log("Set action bar title to 'Home'", Logger.LogLevel.VERBOSE);
        } else logger.log("Failed to set action bar title", Logger.LogLevel.WARNING);

        // Add a listener for the Add button
        findViewById(R.id.btnAddChannel).setOnClickListener(view -> {
            Intent addChannelIntent = new Intent(getApplicationContext(), AddChannelActivity.class);
            addChannelIntent.putExtra("channels_already_added", channelsAlreadyAdded());
            startActivityForResult(addChannelIntent, 738212183); // This code doesn't matter, it just needs to be unique

            logger.log("Successfully Added listener for Add Channel button", Logger.LogLevel.INFO);
        });

        // Get all the data saved and add them to the view
        for (File file : GenericTools.getAllJSONs(this)) {
            Channel channelToAdd = Channel.fromJSON(GenericTools.getFileString(file));
            addChannelToView(channelToAdd);

            logger.log("Added channel " + channelToAdd.getChannelName() + " to home page", Logger.LogLevel.INFO);
        }

        try {
            // Start the periodic service
            Intent periodicIntent = new Intent(this, NotificationChecker.class);
            periodicIntent.putExtra("logger_name", loggerFileName);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 1248812527, periodicIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, 60000, pendingIntent);
            logger.log("Started AlarmManager for channel parser", Logger.LogLevel.INFO);
        } catch (Exception e) {
            logger.log("FAILED TO START PERIODIC PROCESS", Logger.LogLevel.FATAL);
            logger.write();
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        logger.space();
        logger.write();
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
        logger.space();
        logger.log("Returned from activity with request code " + requestCode + " with result " + resultCode, Logger.LogLevel.INFO);

        switch (requestCode) {
            case 738212183: // Returning from adding a channel
                if (resultCode == 1) {
                    // Everything went well, add it to the view
                    Channel returnedChannel = Channel.fromJSON(returnedData.getDataString());
                    logger.log("Channel returned: " + returnedChannel, Logger.LogLevel.VERBOSE);

                    // Save the channel
                    if (!GenericTools.saveAndFetchChannelData(this, returnedChannel)) {
                        GenericTools.showErrorMessage(this, "Could not write channel to filesystem. Please report this error.");
                        logger.log("Failed to save channel.", Logger.LogLevel.ERROR);
                    }
                    // Add the channel to the view
                    addChannelToView(returnedChannel);
                } else if (resultCode == 0)
                    // User cancelled it
                    logger.log("User cancelled activity", Logger.LogLevel.INFO);
                else
                    // Some other error - notify the user
                    logger.log("Unknown result returned from activity", Logger.LogLevel.WARNING);
                break;
            case 6697101: // Returning from editing a channel
                if (resultCode == 1) {
                    // Everything went well - edit the entry
                    Channel returnedChannel = Channel.fromJSON(returnedData.getDataString());
                    String entryToEdit = (String) returnedData.getSerializableExtra("entry_to_edit");
                    logger.log("Updating channel " + returnedChannel.getChannelName(), Logger.LogLevel.INFO);

                    // Start by deleting the old data
                    deleteChannelData(entryToEdit);
                    logger.log("Deleted old channel data", Logger.LogLevel.VERBOSE);

                    // Go through each entry to see if it's the one we want to edit
                    View viewToFind = null;
                    for (int i = 0; i < ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount(); i++) {
                        View currentView = ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildAt(i);

                        if (((TextView) currentView.findViewById(R.id.channelIdTag)).getText().equals(entryToEdit)) {
                            viewToFind = currentView;
                            logger.log("Found entry to edit", Logger.LogLevel.VERBOSE);
                        }
                    }

                    if (viewToFind == null) {
                        logger.log("Could not find entry to edit", Logger.LogLevel.WARNING);
                        logger.space();
                        logger.write();
                        return; // Nothing to edit
                    }

                    // Copy it over, to make it effectively final
                    View viewToEdit = viewToFind;

                    // Update the params of the entry
                    ((TextView) viewToEdit.findViewById(R.id.channelIdTag)).setText(returnedChannel.getChannelID());
                    ((TextView) viewToEdit.findViewById(R.id.labelChannelName)).setText(returnedChannel.getChannelName());

                    // Update the profile pic
                    ImageView profilePic = viewToEdit.findViewById(R.id.imgChannelPic);
                    profilePic.setImageDrawable(GenericTools.getDrawableFromURL(returnedChannel.getPictureURL()));
                    profilePic.setLayoutParams(new LinearLayout.LayoutParams(128, 128));

                    // Update the edit listener
                    viewToEdit.findViewById(R.id.boxEditButton).setOnClickListener(e -> {
                        Intent editChannelIntent = new Intent(getApplicationContext(), AddChannelActivity.class);
                        editChannelIntent.putExtra("entry_to_edit", returnedChannel.getChannelID());
                        editChannelIntent.putExtra("channel_to_edit", returnedChannel);
                        editChannelIntent.putExtra("channels_already_added", channelsAlreadyAdded());
                        logger.log("Asking to edit channel " + returnedChannel.getChannelName(), Logger.LogLevel.INFO);
                        startActivityForResult(editChannelIntent, 6697101); // This code doesn't matter, it just needs to be unique
                    });

                    logger.log("Updated event listeners and entry params", Logger.LogLevel.INFO);

                    // Update the delete listener
                    viewToEdit.findViewById(R.id.boxRemoveButton).setOnClickListener(ev -> {
                        logger.space();
                        logger.log("Attempting to delete channel " + returnedChannel.getChannelName(), Logger.LogLevel.INFO);
                        // Show a confirmation dialogue
                        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(ev.getContext());
                        confirmBuilder.setTitle("Delete " + returnedChannel.getChannelName() + "?");
                        confirmBuilder.setMessage("Are you sure you want to stop receiving notifications from " + returnedChannel.getChannelName() + "?");
                        confirmBuilder.setCancelable(true);
                        confirmBuilder.setPositiveButton("Yes", (dialogInterface, i) -> {
                            logger.log("User confirmed deletion of channel " + returnedChannel.getChannelName(), Logger.LogLevel.INFO);
                            // Now delete the entry
                            ((LinearLayout) viewToEdit.getParent()).removeView(viewToEdit);
                            if (deleteChannelData(returnedChannel.getChannelID())) {
                                GenericTools.showErrorMessage(this, returnedChannel.getChannelName() + " deleted");
                                logger.log("Deleted channel " + returnedChannel.getChannelName());
                            }
                            else {
                                GenericTools.showErrorMessage(this, "Entry " + returnedChannel.getChannelName() + " could not be deleted!");
                                logger.log("Failed to delete channel " + returnedChannel.getChannelName(), Logger.LogLevel.ERROR);
                            }

                            System.out.println("About to remove");
                            // Re-add the "no channel" message if there are no entries left
                            if (((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount() == 0) {
                                // There are none left
                                System.out.println("none left");
                                View noChannelBox = findViewById(R.id.boxNoChannels);
                                if (noChannelBox != null) noChannelBox.setVisibility(View.VISIBLE);
                            }
                        });
                        confirmBuilder.setNegativeButton("Cancel", ((dialogInterface, i) -> {
                            // User cancelled it
                            logger.log("User cancelled deletion of channel " + returnedChannel.getChannelName(), Logger.LogLevel.INFO);
                        }));

                        logger.space();
                        logger.write();
                        confirmBuilder.show();
                    });

                    // Check if the ID was changed - if it was, delete the old JSON
                    // Then write the JSON with the updated info
                    if (!entryToEdit.equals(returnedChannel.getChannelID())) {
                        // Delete the old entry
                        deleteChannelData(entryToEdit);
                        logger.log("Channel ID was changed - deleted old ID", Logger.LogLevel.WARNING);
                    }

                    // Write the new data
                    if (!GenericTools.saveAndFetchChannelData(this, returnedChannel)) {
                        GenericTools.showErrorMessage(this, "Could not write channel to filesystem. Please report this error.");
                        logger.log("Failed to write channel data " + returnedChannel.getChannelName() + " to the filesystem", Logger.LogLevel.ERROR);
                    }
                } else if (resultCode == 0)
                    // User cancelled it. Do nothing
                    logger.log("User cancelled edit activity", Logger.LogLevel.INFO);
                else {
                    // Some other error
                    logger.log("Unknown result returned from activity", Logger.LogLevel.WARNING);
                }
                break;
            default:
                logger.log("Unknown result returned from activity", Logger.LogLevel.WARNING);
                break;
        }
        logger.space();
        logger.write();

        MainActivity.super.onActivityResult(requestCode, resultCode, returnedData);
    }

    /**
     * Add a channel entry to the home page. Automatically categorizes it as favourite vs. all. Assumes the entry has already been written to system
     *
     * @param channel the Channel object to add.
     */
    private void addChannelToView(Channel channel) {
        logger.space();
        logger.log("Asked to add channel " + channel.getChannelName(), Logger.LogLevel.INFO);

        // Check if the "no channel" message is still there - if it is, delete it
        View noChannelBox = findViewById(R.id.boxNoChannels);
        if (noChannelBox.getVisibility() != View.GONE) {
            logger.log("First entry addition requested - hiding 'No Channel' box", Logger.LogLevel.INFO);
            noChannelBox.setVisibility(View.GONE);
        }

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
            logger.log("Asking to edit channel " + channel.getChannelName(), Logger.LogLevel.INFO);
            startActivityForResult(editChannelIntent, 6697101); // This code doesn't matter, it just needs to be unique
        });

        // Add delete listener
        entryToAdd.findViewById(R.id.boxRemoveButton).setOnClickListener(ev -> {
            logger.space();
            logger.log("Attempting to delete channel " + channel.getChannelName(), Logger.LogLevel.INFO);
            // Show a confirmation dialogue
            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(ev.getContext());
            confirmBuilder.setTitle("Delete " + channel.getChannelName() + "?");
            confirmBuilder.setMessage("Are you sure you want to stop receiving notifications from " + channel.getChannelName() + "?");
            confirmBuilder.setCancelable(true);
            confirmBuilder.setPositiveButton("Yes", (dialogInterface, i) -> {
                logger.log("User confirmed deletion", Logger.LogLevel.INFO);
                // Now delete the entry
                ((LinearLayout) entryToAdd.getParent()).removeView(entryToAdd);
                if (deleteChannelData(channel.getChannelID())) {
                    GenericTools.showErrorMessage(this, channel.getChannelName() + " deleted");
                    logger.log("Deleted channel " + channel.getChannelName());
                }
                else {
                    GenericTools.showErrorMessage(this, "Entry " + channel.getChannelName() + " could not be deleted!");
                    logger.log("Failed to delete channel " + channel.getChannelName(), Logger.LogLevel.ERROR);
                }

                // Re-add the "no channel" message if there are no entries left
                if (((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount() == 0) {
                    // There are none left
                    if (noChannelBox.getVisibility() != View.VISIBLE) {
                        noChannelBox.setVisibility(View.VISIBLE);
                        logger.log("Last entry removed - making 'No Channel' box visible", Logger.LogLevel.INFO);
                    }
                }
            });
            confirmBuilder.setNegativeButton("Cancel", ((dialogInterface, i) -> {
                logger.log("User cancelled deletion", Logger.LogLevel.INFO);
            }));

            logger.space();
            logger.write();
            confirmBuilder.show();
        });

        // Add the entry to the screen
        ((LinearLayout) findViewById(R.id.boxChannelsHolder)).addView(entryToAdd);
        logger.log("Added channel to home page", Logger.LogLevel.INFO);
        logger.space();
        logger.write();
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


}