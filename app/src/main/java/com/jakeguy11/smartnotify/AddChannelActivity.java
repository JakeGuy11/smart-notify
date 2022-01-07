package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AddChannelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the main activity as the current view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_channel);

        // Configure the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle("Add a Channel");

        // Create the channel we'll return
        Channel retChannel = new Channel();

        // Start by adding an event listener to the Add Filter button
        // Get the elements we'll interact with
        ImageButton btnAddUploadFilter = findViewById(R.id.btnUploadAdd);
        EditText textUploadFilter = findViewById(R.id.textUploadFilter);

        btnAddUploadFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the text of the filter to add
                String filterToAdd = textUploadFilter.getText().toString();
                // If the text field is empty, do nothing
                if (filterToAdd.equals("")) return;

                // Check each existing filter to make sure it doesn't already exist
                if (retChannel.getUploadKeywords().contains(filterToAdd)) return;

                // We're all good, add the filter to the channel and the view
                retChannel.addUploadKeyword(filterToAdd);
                addFilterKeywordToView(findViewById(R.id.layoutUploadFilters), filterToAdd);
            }
        });

    }

    private void addFilterKeywordToView(LinearLayout layoutToAdd, String filterKeyword) {
        // First, generate our entry and populate it with the filter
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View entryToAdd = inflater.inflate(R.layout.keyword_entry, null);

        // Get the label, change its text
        TextView lblKeyword = entryToAdd.findViewById(R.id.labelKeyword);
        lblKeyword.setText(filterKeyword);

        // Add the label to the view
        layoutToAdd.addView(entryToAdd);

    }

}