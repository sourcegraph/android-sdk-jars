// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.MediaQueueItem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Activity to update properties of items in the queue.
 */
public class QueueUpdateItemsActivity extends BaseQueueActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.queue_update_items_activity,
                R.string.queue_update_activity_title);

        mQueueAdapter = new QueueItemAdapter(this, android.R.layout.simple_spinner_item,
                mQueueListFragment, false, false, true);
        mQueueAdapter.populateAdapterWithIntent(getIntent());
        mQueueListFragment.setListAdapter(mQueueAdapter);
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
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
