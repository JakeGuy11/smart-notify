package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
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

        Channel testCh1 = new Channel("くらげP 1", "WADATAKEAKI");
        System.out.println(testCh1.getPictureURL());
        testCh1.favourited = true;
        addChannelToView(testCh1);

        Channel testCh2 = new Channel("くらげP 2", "WADATAKEAKI");
        System.out.println(testCh2.getPictureURL());
        testCh2.favourited = true;
        addChannelToView(testCh2);

        Channel testCh3 = new Channel("くらげP 3", "WADATAKEAKI");
        System.out.println(testCh3.getPictureURL());
        testCh3.favourited = true;
        addChannelToView(testCh3);

        Channel testCh4 = new Channel("くらげP 4", "WADATAKEAKI");
        System.out.println(testCh4.getPictureURL());
        addChannelToView(testCh4);

        Channel testCh5 = new Channel("くらげP 5", "WADATAKEAKI");
        System.out.println(testCh5.getPictureURL());
        addChannelToView(testCh5);

        Channel testCh6 = new Channel("くらげP 6", "WADATAKEAKI");
        System.out.println(testCh6.getPictureURL());
        addChannelToView(testCh6);

        Channel testCh7 = new Channel("Ex + Fire 7", "https://www.youtube.com/channel/UCVovvq34gd0ps5cVYNZrc7A");
        System.out.println(testCh7.getPictureURL());
        addChannelToView(testCh7);

        Channel testCh8 = new Channel("Ex + Fire 8", "https://www.youtube.com/channel/UCVovvq34gd0ps5cVYNZrc7A");
        System.out.println(testCh8.getPictureURL());
        addChannelToView(testCh8);

        Channel testCh9 = new Channel("Ex + Fire 9", "https://www.youtube.com/channel/UCVovvq34gd0ps5cVYNZrc7A");
        System.out.println(testCh9.getPictureURL());
        addChannelToView(testCh9);

        Channel testCh10 = new Channel("Ex + Fire 10", "https://www.youtube.com/channel/UCVovvq34gd0ps5cVYNZrc7A");
        System.out.println(testCh10.getPictureURL());
        addChannelToView(testCh10);
    }

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