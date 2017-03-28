// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.MediaQueueItem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Activity to insert a single item into the queue, and immediately start playing it.
 */
public class QueueInsertAndPlayItemActivity extends BaseQueueActivity {

    private static final String START_TIME_DIALOG_TAG = "StartTimeDialogFragment";

    private TextView mStartTimeSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.queue_insert_and_play_item_activity,
                R.string.queue_insert_and_play_item_activity_title);

        View startTimeRow = findViewById(R.id.start_time_row);
        TextView startTimeTitle = (TextView) startTimeRow.findViewById(android.R.id.text1);
        startTimeTitle.setText(R.string.edit_queue_item_activity_start_time_title);
        mStartTimeSubtitle = (TextView) startTimeRow.findViewById(android.R.id.text2);
        mStartTimeSubtitle.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        startTimeRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialog = new EditTextDialog(R.string.enter_start_time) {
                    @Override
                    protected void onDialogPositiveClick(String text) {
                        mStartTimeSubtitle.setText(text);
                    }
                };
                dialog.show(getSupportFragmentManager(), START_TIME_DIALOG_TAG);
            }
        });

        mQueueAdapter = new QueueItemAdapter(this, android.R.layout.simple_spinner_item,
                mQueueListFragment, true, false, true);
        mQueueAdapter.populateAdapterWithIntent(getIntent());
        mQueueListFragment.setListAdapter(mQueueAdapter);

        final Button addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQueueListFragment.onAddNewItemButtonClicked();
                // Disable the add button so only one item can be inserted.
                addButton.setEnabled(false);
            }
        });
    }

    @Override
    protected void onOkButtonClick() {
        Intent intent = new Intent();
        MediaQueueItem[] insertedItems = mQueueListFragment.getInsertedItems();
        if (insertedItems.length == 0) {
            finish();
            return;
        }
        intent.putExtra(EXTRA_INSERTED_ITEM, insertedItems[0].toJson().toString());
        MediaQueueItem selectedItem = mQueueAdapter.getSelectedItem();
        int insertBeforeItemId =
                (selectedItem != null) ? selectedItem.getItemId() : MediaQueueItem.INVALID_ITEM_ID;
        intent.putExtra(EXTRA_INSERT_BEFORE_ITEM_ID, insertBeforeItemId);
        try {
            double startTime = Double.valueOf(mStartTimeSubtitle.getText().toString());
            intent.putExtra(EXTRA_START_TIME, startTime);
        } catch (NumberFormatException e) {
            // do not put in startTime.
        }
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
