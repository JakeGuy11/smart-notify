package com.jakeguy11.smartnotify;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
        Channel startingChannel = (Channel) getIntent().getSerializableExtra("channel_to_edit");
        // If there is no starting channel, create a new one. If there is, populate the form with its data.
        if (startingChannel == null)
            startingChannel = new Channel();
        else
            populateForm(startingChannel);

        // Clone our startingChannel to another channel that can effectively be final
        Channel retChannel = new Channel(startingChannel);

        // Start by adding an event listener to the Add Filter button
        // Get the elements we'll interact with
        ImageButton btnAddUploadFilter = findViewById(R.id.btnUploadAdd);
        ImageButton btnClearUploadFilter = findViewById(R.id.btnUploadClear);
        EditText textUploadFilter = findViewById(R.id.textUploadFilter);

        // Add handling for when a filter is added
        btnAddUploadFilter.setOnClickListener(v -> {
            // Get the text of the filter to add
            String filterToAdd = textUploadFilter.getText().toString();
            // If the text field is empty, do nothing
            if (filterToAdd.equals("")) return;

            // Check each existing filter to make sure it doesn't already exist
            if (retChannel.getUploadKeywords().contains(filterToAdd)) return;

            // Generate the entry
            retChannel.addUploadKeyword(filterToAdd);
            View entry = generateFilterKeyword(filterToAdd);

            // Add a listener to delete the filter keyword
            ImageView btnDeleteFilter = entry.findViewById(R.id.imgDeleteKeyword);
            btnDeleteFilter.setOnClickListener(view -> {
                // Delete the entry
                retChannel.removeUploadKeyword(filterToAdd);
                ((ViewManager)entry.getParent()).removeView(entry);
            });

            // Add the view to the page
            ((LinearLayout)findViewById(R.id.layoutUploadFilters)).addView(entry);

            // Clear the text field
            textUploadFilter.setText("");
        });

        // Just a simple thing here, clear the text box when the X is clicked
        btnClearUploadFilter.setOnClickListener(view -> {
            textUploadFilter.setText("");
        });

        // Do both of the above, but for streams
        ImageButton btnAddStreamFilter = findViewById(R.id.btnStreamAdd);
        ImageButton btnClearStreamFilter = findViewById(R.id.btnStreamClear);
        EditText textStreamFilter = findViewById(R.id.textStreamFilter);

        // Add the stream listeners
        btnAddStreamFilter.setOnClickListener(v -> {
            // Get the text of the filter to add
            String filterToAdd = textStreamFilter.getText().toString();
            // If the text field is empty, do nothing
            if (filterToAdd.equals("")) return;

            // Check each existing filter to make sure it doesn't already exist
            if (retChannel.getStreamKeywords().contains(filterToAdd)) return;

            // We're all good, add the filter to the channel and the view
            retChannel.addStreamKeyword(filterToAdd);
            View entry = generateFilterKeyword(filterToAdd);

            // Add a listener to delete the filter keyword
            ImageView btnDeleteFilter = entry.findViewById(R.id.imgDeleteKeyword);
            btnDeleteFilter.setOnClickListener(view -> {
                // Delete the entry
                retChannel.removeStreamKeyword(filterToAdd);
                ((ViewManager)entry.getParent()).removeView(entry);
            });

            // Add the view to the page
            ((LinearLayout)findViewById(R.id.layoutStreamFilters)).addView(entry);

            // Clear the text field
            textStreamFilter.setText("");
        });
        btnClearStreamFilter.setOnClickListener(view -> {
            textStreamFilter.setText("");
        });

        // Add an event listener for the cancel button
        findViewById(R.id.btnCancelChannel).setOnClickListener(view -> {
            setResult(1);
            finish();
        });

        // Add an event listener for the save button
        findViewById(R.id.btnSaveChannel).setOnClickListener(view -> {
            // Get all the form elements as variables
            EditText txtName = findViewById(R.id.textName);
            EditText txtID = findViewById(R.id.textID);
            CheckBox chkFavourite = findViewById(R.id.chkFavourite);
            CheckBox chkUploadNotifs = findViewById(R.id.chkUploadNotifs);
            CheckBox chkUploadFilter = findViewById(R.id.chkUploadFilter);
            CheckBox chkStreamNotifs = findViewById(R.id.chkStreamNotifs);
            CheckBox chkStreamFilter = findViewById(R.id.chkStreamFilter);

            // Check to make sure the name and channel are non-null
            boolean nameNull = txtName.getText().toString().equals("");
            boolean channelNull = txtID.getText().toString().equals("");
            if (nameNull || channelNull) {
                // Show an error message, quit
                if (channelNull) showErrorMessage("You cannot have an empty channel!");
                if (nameNull) showErrorMessage("You cannot have an empty name!");
                return;
            }

            // Assign all the properties to the channel object
            // First verify that the channel ID is valid
            if (!retChannel.setChannelID(txtID.getText().toString())) {
                showErrorMessage("That is not a valid channel ID/URL.");
                return;
            }
            retChannel.setChannelName(txtName.getText().toString());
            retChannel.setFavourited(chkFavourite.isChecked());
            retChannel.setNotifyUploads(chkUploadNotifs.isChecked());
            retChannel.setNotifyStreams(chkStreamNotifs.isChecked());
            retChannel.setFilterUploads(chkUploadFilter.isChecked());
            retChannel.setFilterStreams(chkStreamFilter.isChecked());

            // Create a URI to store the channel in, return it
            Intent retData = new Intent();
            retData.setData(Uri.parse(retChannel.toString()));
            setResult(0, retData);
            finish();
        });

    }

    private void showErrorMessage(String msg) {
        int length = Toast.LENGTH_LONG;
        if (msg.length() <= 30) length = Toast.LENGTH_SHORT;
        Toast.makeText(getApplicationContext(),msg, length).show();
    }

    private void populateForm(Channel channel) {
        // Set the channel name
        ((EditText)findViewById(R.id.textName)).setText(channel.getChannelName());

        // Set the channel ID
        ((EditText)findViewById(R.id.textID)).setText(channel.getChannelID());

        // Set the favourite
        ((CheckBox)findViewById(R.id.chkFavourite)).setChecked(channel.isFavourited());

        // Set the upload notification setting
        ((CheckBox)findViewById(R.id.chkUploadNotifs)).setChecked(channel.getNotifyUploads());

        // Set the upload filter enabled setting
        ((CheckBox)findViewById(R.id.chkUploadFilter)).setChecked(channel.getFilterUploads());

        // Set the upload filters
        EditText uploadField = findViewById(R.id.textUploadFilter);
        ImageButton uploadButton = findViewById(R.id.btnUploadAdd);
        for (String item : channel.getUploadKeywords()) {
            uploadField.setText(item);
            uploadButton.performClick();
        }
        uploadField.setText("");

        // Set the stream notification setting
        ((CheckBox)findViewById(R.id.chkStreamNotifs)).setChecked(channel.getNotifyStreams());

        // Set the stream filter enabled setting
        ((CheckBox)findViewById(R.id.chkStreamFilter)).setChecked(channel.getFilterStreams());

        // Set the stream filters
        EditText streamField = findViewById(R.id.textStreamFilter);
        ImageButton streamButton = findViewById(R.id.btnStreamAdd);
        for (String item : channel.getStreamKeywords()) {
            streamField.setText(item);
            streamButton.performClick();
        }
        streamField.setText("");

    }

    /**
     * Generate a filter keyword to a view.
     *
     * @param filterKeyword The keyword to set the text to.
     */
    private View generateFilterKeyword(String filterKeyword) {
        // First, generate our entry and populate it with the filter
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View entryToAdd = inflater.inflate(R.layout.keyword_entry, null);

        // Get the label, change its text
        TextView lblKeyword = entryToAdd.findViewById(R.id.labelKeyword);
        lblKeyword.setText(filterKeyword);

        return entryToAdd;

    }

}