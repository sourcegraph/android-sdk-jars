// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Gravity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QueueListFragment extends ListFragment implements QueueItemAdapter.Listener {

    private static final String MEDIA_SELECTION_DIALOG_TAG = "media_selection";
    private static final int REQUEST_CODE_EDIT_ITEM = 1;

    private MediaSelectionDialog mMediaSelectionDialog;
    private List<MediaQueueItem> mInsertedItems;

    @Override
     public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mInsertedItems = new ArrayList<>();
        addQueueHeader();
    }

    @Override
    public void onSelectedItemChanged(int position) {
        int headerCount = getListView().getHeaderViewsCount();
        int footerCount = getListView().getFooterViewsCount();
        int count = getListView().getChildCount();
        int targetPosition = headerCount + position;

        for (int i = headerCount; i < count - footerCount; ++i) {
            View v = getListView().getChildAt(i);
            RadioButton button = (RadioButton) v.findViewById(R.id.radio_button);
            button.setChecked(targetPosition == i);
        }
    }

    @Override
    public void onItemClicked(int position) {
        MediaQueueItem item = ((QueueItemAdapter) getListAdapter()).getItem(position);

        Intent intent = new Intent(getActivity(), EditQueueItemActivity.class);
        intent.putExtra(EditQueueItemActivity.EXTRA_ITEM, item.toJson().toString());
        intent.putExtra(EditQueueItemActivity.EXTRA_POSITION, position);

        startActivityForResult(intent, REQUEST_CODE_EDIT_ITEM);
    }

    public MediaQueueItem[] getInsertedItems() {
        return mInsertedItems.toArray(new MediaQueueItem[mInsertedItems.size()]);
    }

    private void addQueueHeader() {
        TextView header = new TextView(getActivity());
        header.setText(R.string.media_queue_title);
        header.setPadding(0, 50, 0, 20);
        header.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        header.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
        getListView().addHeaderView(header);
    }

    public void onAddNewItemButtonClicked() {
        if (mMediaSelectionDialog == null) {
            mMediaSelectionDialog = new MediaSelectionDialog(getActivity()) {
                @Override
                protected void onItemSelected(final MediaInfo item) {
                    onItemAdded(item);
                }
            };
        }
        mMediaSelectionDialog.show(getFragmentManager(), MEDIA_SELECTION_DIALOG_TAG);
    }

    private void onItemAdded(MediaInfo item) {
        MediaQueueItem queueItem = new MediaQueueItem.Builder(item).build();
        QueueItemAdapter queueItemAdapter = ((QueueItemAdapter) getListAdapter());
        int insertBeforeItemId = MediaQueueItem.INVALID_ITEM_ID;
        int selectedPosition = queueItemAdapter.getSelectedPosition();
        if (selectedPosition >= 0 && selectedPosition < queueItemAdapter.getCount()) {
            insertBeforeItemId = queueItemAdapter.getItem(selectedPosition).getItemId();
        }
        if (insertBeforeItemId == MediaQueueItem.INVALID_ITEM_ID) {
            queueItemAdapter.add(queueItem);
        } else {
            int indexToInsert = -1;
            for (int i = 0; i < queueItemAdapter.getCount(); i++) {
                if (queueItemAdapter.getItem(i).getItemId() == insertBeforeItemId) {
                    indexToInsert = i;
                    break;
                }
            }
            if (indexToInsert != -1) {
                queueItemAdapter.insert(queueItem, indexToInsert);
            }
            queueItemAdapter.setSelectedPosition(selectedPosition + 1);
        }
        queueItemAdapter.notifyDataSetChanged();
        mInsertedItems.add(queueItem);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_EDIT_ITEM) {
            int position = data.getIntExtra(EditQueueItemActivity.EXTRA_POSITION, -1);
            if (position < 0) {
                return;
            }
            String itemString = data.getStringExtra(EditQueueItemActivity.EXTRA_ITEM);
            MediaQueueItem newItem;
            try {
                newItem = new MediaQueueItem.Builder(new JSONObject(itemString)).build();
            } catch (JSONException e) {
                return;
            }
            MediaQueueItem oldItem = ((QueueItemAdapter) getListAdapter()).getItem(position);
            ((QueueItemAdapter) getListAdapter()).remove(oldItem);
            ((QueueItemAdapter) getListAdapter()).insert(newItem, position);
        }
    }
}
