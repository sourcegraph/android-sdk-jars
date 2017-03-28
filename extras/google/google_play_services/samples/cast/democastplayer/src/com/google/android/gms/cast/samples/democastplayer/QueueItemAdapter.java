// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * An array adapter of the queue items. Keeps track of the selected (RadioButton) and checked
 * (CheckBox) items.
 */
class QueueItemAdapter extends ArrayAdapter<MediaQueueItem> {
    private final Listener mListener;
    private final int mRadioButtonsVisibility;
    private final int mCheckBoxesVisibility;
    private final boolean mClickable;
    private int mSelectedPosition = -1;
    private Set<Integer> mCheckedPositions = new HashSet<>();

    public interface Listener {
        void onSelectedItemChanged(int position);
        void onItemClicked(int position);
    }

    public QueueItemAdapter(Context context, int resource, Listener listener,
            boolean showRadioButtons, boolean showCheckBoxes, boolean clickable) {
        super(context, resource);
        mListener = listener;
        mRadioButtonsVisibility = showRadioButtons ? View.VISIBLE : View.GONE;
        mCheckBoxesVisibility = showCheckBoxes ? View.VISIBLE : View.GONE;
        mClickable = clickable;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public void setSelectedPosition(int position) {
        mSelectedPosition = position;
        if (mListener != null) {
            mListener.onSelectedItemChanged(position);
        }
    }

    public MediaQueueItem getSelectedItem() {
        if (mSelectedPosition != -1) {
            return getItem(mSelectedPosition);
        }
        return null;
    }

    public void setSelectItemId(int itemId) {
        if (itemId != MediaQueueItem.INVALID_ITEM_ID) {
            for (int i = 0; i < getCount(); i++) {
                if (getItem(i).getItemId() == itemId) {
                    setSelectedPosition(i);
                    return;
                }
            }
        }
    }

    public int[] getCheckedItemIds() {
        int[] checkedItemIds = new int[mCheckedPositions.size()];
        int i = 0;
        for (int position : mCheckedPositions) {
            checkedItemIds[i++] = getItem(position).getItemId();
        }
        return checkedItemIds;
    }

    public void populateAdapterWithIntent(Intent intent) {
        if (intent.hasExtra(BaseQueueActivity.EXTRA_ITEMS)) {
            String[] itemStrings = intent.getStringArrayExtra(BaseQueueActivity.EXTRA_ITEMS);
            for (String itemString : itemStrings) {
                try {
                    add(new MediaQueueItem.Builder(new JSONObject(itemString)).build());
                } catch (JSONException e) {
                    // Should never happen.
                }
            }
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            view = inflater.inflate(R.layout.queue_item, parent, false);
        }

        RadioButton radioButton = (RadioButton) view.findViewById(R.id.radio_button);
        radioButton.setVisibility(mRadioButtonsVisibility);
        if (position == mSelectedPosition) {
            radioButton.setChecked(true);
        }
        radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedPosition != position) {
                    mSelectedPosition = position;
                    if (mListener != null) {
                        mListener.onSelectedItemChanged(position);
                    }
                }
            }
        });

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.check_box);
        checkBox.setVisibility(mCheckBoxesVisibility);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCheckedPositions.add(position);
            }
        });

        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        TextView textView = (TextView) view.findViewById(R.id.text);

        MediaQueueItem item = getItem(position);
        MediaInfo mediaInfo = item.getMedia();
        MediaMetadata metadata = mediaInfo.getMetadata();
        if (metadata == null) {
            textView.setText(R.string.unsupported_item);
            imageView.setImageBitmap(null);
        } else {
            switch (metadata.getMediaType()) {
                case MediaMetadata.MEDIA_TYPE_PHOTO:
                    imageView.setImageResource(R.drawable.type_image);
                    break;
                case MediaMetadata.MEDIA_TYPE_MUSIC_TRACK:
                    imageView.setImageResource(R.drawable.type_audio);
                    break;
                case MediaMetadata.MEDIA_TYPE_MOVIE:
                case MediaMetadata.MEDIA_TYPE_TV_SHOW:
                    imageView.setImageResource(R.drawable.type_video);
                    break;
                default:
                    imageView.setImageBitmap(null);
            }

            textView.setText(metadata.getString(MediaMetadata.KEY_TITLE));
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onItemClicked(position);
            }
        });
        view.setClickable(mClickable);

        return view;
    }

    @Override
    public void clear() {
        super.clear();
    }
}
