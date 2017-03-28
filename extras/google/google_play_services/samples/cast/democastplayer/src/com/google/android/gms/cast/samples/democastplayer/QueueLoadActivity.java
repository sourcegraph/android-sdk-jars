// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.cast.MediaQueueItem;

/**
 * Activity to load a queue.
 */
public class QueueLoadActivity extends BaseQueueActivity {
    private static final String START_TIME_DIALOG_TAG = "StartTimeDialogFragment";

    private Spinner mRepeatModeSpinner;
    private TextView mStartTimeSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.queue_load_activity,
                R.string.queue_load_activity_title);

        mRepeatModeSpinner = (Spinner) findViewById(R.id.repeat_mode_spinner);

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
        mQueueAdapter.setSelectedPosition(0);
        mQueueListFragment.setListAdapter(mQueueAdapter);

        Button addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQueueListFragment.onAddNewItemButtonClicked();
            }
        });
    }

    @Override
    protected void onOkButtonClick() {
        int count = mQueueAdapter.getCount();
        if (count <= 0) {
            finish();
            return;
        }
        String[] itemStrings = new String[count];
        for (int i = 0; i < count; ++i) {
            MediaQueueItem item = mQueueAdapter.getItem(i);
            itemStrings[i] = item.toJson().toString();
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ITEMS, itemStrings);
        intent.putExtra(EXTRA_REPEAT_MODE, mRepeatModeSpinner.getSelectedItemPosition());
        intent.putExtra(EXTRA_START_INDEX, mQueueAdapter.getSelectedPosition());
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
