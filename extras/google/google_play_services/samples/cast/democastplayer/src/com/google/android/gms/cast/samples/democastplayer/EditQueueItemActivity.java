// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.MediaQueueItem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity to modify a media item in the queue.
 */
public class EditQueueItemActivity extends ActionBarActivity {

    public static final String EXTRA_POSITION = "extra_position";
    public static final String EXTRA_ITEM = "extra_item";

    private static final String START_TIME_DIALOG_TAG = "StartTimeDialogFragment";
    private static final String PRELOAD_TIME_DIALOG_TAG = "PreloadTimeDialogFragment";
    private static final String PLAYBACK_DURATION_DIALOG_TAG = "PlaybackDurationDialogFragment";

    private MediaQueueItem mItem;
    private MediaQueueItem.Builder mNewItemBuilder;
    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_queue_item_activity);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.edit_queue_item_activity_title);

        Intent intent = getIntent();
        mPosition = intent.getIntExtra(EXTRA_POSITION, -1);
        if (mPosition < 0) {
            return;
        }
        try {
            mItem = new MediaQueueItem.Builder(new JSONObject(intent.getStringExtra(EXTRA_ITEM)))
                    .build();
            mNewItemBuilder = new MediaQueueItem.Builder(mItem);
        } catch (JSONException e) {
            return;
        }

        // Item ID row.
        View itemIdRow = findViewById(R.id.item_id_row);
        TextView itemIdTitle = (TextView) itemIdRow.findViewById(android.R.id.text1);
        itemIdTitle.setText(R.string.edit_queue_item_activity_item_id_title);
        TextView itemIdSubtitle = (TextView) itemIdRow.findViewById(android.R.id.text2);
        itemIdSubtitle.setText(String.valueOf(mItem.getItemId()));

        // Auto play row.
        CheckBox autoPlayCheckbox = (CheckBox) findViewById(R.id.auto_play_checkbox);
        autoPlayCheckbox.setChecked(mItem.getAutoplay());
        autoPlayCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNewItemBuilder.setAutoplay(((CheckBox) view).isChecked());
            }
        });

        // Start time row.
        View startTimeRow = findViewById(R.id.start_time_row);
        TextView startTimeTitle = (TextView) startTimeRow.findViewById(android.R.id.text1);
        startTimeTitle.setText(R.string.edit_queue_item_activity_start_time_title);
        final TextView startTimeSubtitle = (TextView) startTimeRow.findViewById(android.R.id.text2);
        startTimeSubtitle.setText(String.valueOf(mItem.getStartTime()));
        startTimeRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialog = new EditTextDialog(R.string.enter_start_time) {
                    @Override
                    protected void onDialogPositiveClick(String text) {
                        startTimeSubtitle.setText(text);
                        mNewItemBuilder.setStartTime(Double.valueOf(text));
                    }
                };
                dialog.show(getSupportFragmentManager(), START_TIME_DIALOG_TAG);
            }
        });

        // Preload time row.
        View preloadTimeRow = findViewById(R.id.preload_time_row);
        TextView preloadTimeTitle = (TextView) preloadTimeRow.findViewById(android.R.id.text1);
        preloadTimeTitle.setText(R.string.edit_queue_item_activity_preload_time_title);

        final TextView preloadTimeSubtitle =
                (TextView) preloadTimeRow.findViewById(android.R.id.text2);
        preloadTimeSubtitle.setText(String.valueOf(mItem.getPreloadTime()));
        preloadTimeRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialog = new EditTextDialog(R.string.enter_preload_time) {
                    @Override
                    protected void onDialogPositiveClick(String text) {
                        preloadTimeSubtitle.setText(text);
                        mNewItemBuilder.setPreloadTime(Double.valueOf(text));
                    }
                };
                dialog.show(getSupportFragmentManager(), PRELOAD_TIME_DIALOG_TAG);
            }
        });

        // Playback duration row.
        View playbackDurationRow = findViewById(R.id.playback_duration_row);
        TextView playbackDurationTitle =
                (TextView) playbackDurationRow.findViewById(android.R.id.text1);
        playbackDurationTitle.setText(R.string.edit_queue_item_activity_playback_duration_title);

        final TextView playbackDurationTitleSubtitle =
                (TextView) playbackDurationRow.findViewById(android.R.id.text2);
        playbackDurationTitleSubtitle.setText(String.valueOf(mItem.getPlaybackDuration()));
        playbackDurationRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialog = new EditTextDialog(R.string.enter_playback_duration) {
                    @Override
                    protected void onDialogPositiveClick(String text) {
                        playbackDurationTitleSubtitle.setText(text);
                        mNewItemBuilder.setPlaybackDuration(Double.valueOf(text));
                    }
                };
                dialog.show(getSupportFragmentManager(), PLAYBACK_DURATION_DIALOG_TAG);
            }
        });

        // Control row.
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        Button okButton = (Button) findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkPressed();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED, new Intent());
        finish();
    }

    private void onOkPressed () {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_POSITION, mPosition);
        intent.putExtra(EXTRA_ITEM, mNewItemBuilder.build().toJson().toString());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
