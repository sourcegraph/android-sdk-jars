// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.MediaQueueItem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Activity to insert items into the queue.
 */
public class QueueInsertItemsActivity extends BaseQueueActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.queue_insert_items_activity,
                R.string.queue_insert_activity_title);

        mQueueAdapter = new QueueItemAdapter(this, android.R.layout.simple_spinner_item,
                mQueueListFragment, true, false, true);
        mQueueAdapter.populateAdapterWithIntent(getIntent());
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
        Intent intent = new Intent();
        MediaQueueItem selectedItem = mQueueAdapter.getSelectedItem();
        int insertBeforeItemId =
                (selectedItem != null) ? selectedItem.getItemId() : MediaQueueItem.INVALID_ITEM_ID;
        intent.putExtra(EXTRA_INSERT_BEFORE_ITEM_ID, insertBeforeItemId);
        MediaQueueItem[] insertedItems = mQueueListFragment.getInsertedItems();
        int count = insertedItems.length;
        String[] itemStrings = new String[count];
        for (int i = 0; i < count; ++i) {
            itemStrings[i] = insertedItems[i].toJson().toString();
        }
        intent.putExtra(EXTRA_ITEMS, itemStrings);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
