package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class AddChannelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the main activity as the current view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_channel);

        // Configure the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle("Add a Channel");

        // Create a sample channel to return
        Channel ch = new Channel("Baelz", "https://www.youtube.com/channel/UCgmPnx-EEeOrZSg5Tiw7ZRQ");
        ch.favourited = true;

        // Create the intent to return
        Intent returnChannel = new Intent();
        returnChannel.setData(Uri.parse(ch.toString()));
        setResult(0, returnChannel);

        finish();

    }
}