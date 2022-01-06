package com.jakeguy11.smartnotify;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedData) {
        switch (requestCode) {
            case 738212183: // Returning from the addChannel activity
                if (resultCode == 0) {
                    // Everything went well, add it to the view
                    Channel returnedChannel = Channel.fromJSON(returnedData.getDataString());
                    addChannelToView(returnedChannel);
                } else if (resultCode == 1) {
                    // User cancelled it. Do nothing, maybe add a message in the future
                    System.out.println("User cancelled the addition");
                } else {
                    // Some other error - notify the user
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
        // Create an inflater so we can customize resources
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Get then customize an entry
        android.view.View entryToAdd = inflater.inflate(R.layout.channel_entry, null);
        ImageView profilePic = entryToAdd.findViewById(R.id.imgChannelPic);
        profilePic.setImageDrawable(getDrawableFromURL(channel.getPictureURL()));
        profilePic.setLayoutParams(new LinearLayout.LayoutParams(128, 128));
        ((TextView) entryToAdd.findViewById(R.id.labelChannelName)).setText(channel.getChannelName());
        if (channel.favourited) {
            // Change the favourited button, add to the favourite view
            ((ImageView) entryToAdd.findViewById(R.id.imageHeart)).setImageDrawable(getResources().getDrawable(R.drawable.heart_checked));
            ((LinearLayout) findViewById(R.id.boxFavChannels)).addView(entryToAdd);
            return;
        }
        ((LinearLayout) findViewById(R.id.boxAllChannels)).addView(entryToAdd);
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
            Drawable returnDrawable = Drawable.createFromStream(urlStream, null);
            return returnDrawable;
        } catch (Exception e) {
            return null;
        }
    }
}