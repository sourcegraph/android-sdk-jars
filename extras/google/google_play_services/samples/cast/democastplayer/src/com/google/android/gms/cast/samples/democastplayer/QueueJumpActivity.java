// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.MediaQueueItem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;

/**
 * Activity to jump to an item in the queue.
 */
public class QueueJumpActivity extends BaseQueueActivity {

    private static final String START_TIME_DIALOG_TAG = "StartTimeDialogFragment";

    private TextView mStartTimeSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.queue_jump_activity,
                R.string.queue_jump_activity_title);

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
                mQueueListFragment, true, false, false);
        mQueueAdapter.populateAdapterWithIntent(getIntent());
        mQueueAdapter.setSelectItemId(
                getIntent().getIntExtra(EXTRA_CURRENT_ITEM_ID, MediaQueueItem.INVALID_ITEM_ID));
        mQueueListFragment.setListAdapter(mQueueAdapter);
    }

    @Override
    protected void onOkButtonClick() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CURRENT_ITEM_ID, mQueueAdapter.getSelectedItem().getItemId());
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
