package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the main activity as the current view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle("Home");

        Channel testCh = new Channel();
        testCh.setChannelName("くらげP");
        testCh.setChannelID("WADATAKEAKI"); // That's the channel ID - URL is https://www.youtube.com/c/WADATAKEAKI
        System.out.println(testCh.getPictureURL());
    }
}