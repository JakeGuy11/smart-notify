package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.InputStream;
import java.net.URL;

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
            startActivityForResult(addChannelIntent, 738212183); // This code doesn't matter, it just needs to be unique
        });

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
            case 738212183: // Returning from the addChannel activity
                if (resultCode == 0) {
                    // Everything went well, add it to the view
                    Channel returnedChannel = Channel.fromJSON(returnedData.getDataString());
                    System.out.println("Channel returned\n\n" + returnedChannel);

                    // Make sure the ID doesn't exist
                    for (int i = 0; i < ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount(); i++) {
                        View currentView = ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildAt(i);

                        if (((TextView)currentView.findViewById(R.id.channelIdTag)).getText().equals(returnedChannel.getChannelID())) {
                            System.out.println("ID already exists!");
                            return;
                        }
                    }

                    // Add the channel
                    addChannelToView(returnedChannel);
                } else if (resultCode == 1) {
                    // User cancelled it. Do nothing, maybe add a message in the future
                    System.out.println("User cancelled the addition");
                } else {
                    // Some other error - notify the user
                    System.out.println("Something went terribly, terribly wrong. You should not be reading this.");
                }
                break;
            case 6697101:
                if (resultCode == 0) {
                    // Everything went well - edit the entry
                    Channel returnedChannel = Channel.fromJSON(returnedData.getDataString());
                    String entryToEdit = (String) returnedData.getSerializableExtra("entry_to_edit");

                    // Make sure the new channel ID doesn't exist
                    for (int i = 0; i < ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount(); i++) {
                        View currentView = ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildAt(i);

                        if (((TextView)currentView.findViewById(R.id.channelIdTag)).getText().equals(returnedChannel.getChannelID())) {
                            System.out.println("ID already exists!");
                            return;
                        }
                    }

                    // Go through each entry to see if it's the one we want to edit
                    View viewToEdit = null;
                    for (int i = 0; i < ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildCount(); i++) {
                        View currentView = ((LinearLayout) findViewById(R.id.boxChannelsHolder)).getChildAt(i);

                        if (((TextView)currentView.findViewById(R.id.channelIdTag)).getText().equals(entryToEdit))
                            viewToEdit = currentView;
                    }

                    if (viewToEdit == null) return; // Nothing to edit

                    // Update the params of the entry
                    ((TextView)viewToEdit.findViewById(R.id.channelIdTag)).setText(returnedChannel.getChannelID());
                    ((TextView)viewToEdit.findViewById(R.id.labelChannelName)).setText(returnedChannel.getChannelName());

                    // Update the profile pic
                    ImageView profilePic = (ImageView) viewToEdit.findViewById(R.id.imgChannelPic);
                    profilePic.setImageDrawable(getDrawableFromURL(returnedChannel.getPictureURL()));
                    profilePic.setLayoutParams(new LinearLayout.LayoutParams(128, 128));

                    // Done, I think?
                } else if (resultCode == 1) {
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
     * Add a channel entry to the home page. Automatically categorizes it as favourite vs. all.
     *
     * @param channel the Channel object to add.
     */
    private void addChannelToView(Channel channel) {
        // Check if the "no channel" message is still there - if it is, delete it
        View noChannelBox = findViewById(R.id.boxNoChannels);
        if (noChannelBox != null) ((ViewManager) noChannelBox.getParent()).removeView(noChannelBox);

        // Create an inflater so we can customize resources
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Get then customize an entry
        View entryToAdd = (View) inflater.inflate(R.layout.channel_entry, null);
        ImageView profilePic = entryToAdd.findViewById(R.id.imgChannelPic);
        profilePic.setImageDrawable(getDrawableFromURL(channel.getPictureURL()));
        profilePic.setLayoutParams(new LinearLayout.LayoutParams(128, 128));
        ((TextView) entryToAdd.findViewById(R.id.labelChannelName)).setText(channel.getChannelName());
        ((TextView) entryToAdd.findViewById(R.id.channelIdTag)).setText(channel.getChannelID());

        // Add edit listeners
        entryToAdd.findViewById(R.id.boxSettingsButton).setOnClickListener(e -> {
            Intent editChannelIntent = new Intent(getApplicationContext(), AddChannelActivity.class);
            editChannelIntent.putExtra("entry_to_edit", channel.getChannelID());
            editChannelIntent.putExtra("channel_to_edit", channel);
            startActivityForResult(editChannelIntent, 6697101); // This code doesn't matter, it just needs to be unique
        });

        // Add the entry to the screen
        ((LinearLayout) findViewById(R.id.boxChannelsHolder)).addView(entryToAdd);
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
}