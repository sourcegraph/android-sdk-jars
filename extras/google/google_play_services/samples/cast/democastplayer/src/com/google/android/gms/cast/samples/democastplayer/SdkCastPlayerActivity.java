// Copyright 2013 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Activity to cast media to a Cast device using the MediaRouter APIs for discovery, and the
 * Cast SDK for media selection and playback.
 */
public class SdkCastPlayerActivity extends BaseCastPlayerActivity {
    private static final String TAG = "SdkCastPlayerActivity";

    // Request code to use when launching the resolution activity.
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private static final int RESULT_CODE_QUEUE_LOAD = 1;
    private static final int RESULT_CODE_QUEUE_INSERT = 2;
    private static final int RESULT_CODE_QUEUE_UPDATE = 3;
    private static final int RESULT_CODE_QUEUE_REMOVE = 4;
    private static final int RESULT_CODE_QUEUE_JUMP = 5;
    private static final int RESULT_CODE_QUEUE_INSERT_AND_PLAY = 6;

    private static final String START_TIME_DIALOG_TAG = "StartTimeDialogFragment";

    private Button mLoadQueueButton;
    private Button mInsertQueueButton;
    private Button mUpdateQueueButton;
    private Button mRemoveQueueButton;
    private Button mQueueJumpButton;
    private Button mQueuePrevButton;
    private Button mQueueNextButton;
    private Button mQueueInsertAndPlayItemButton;
    private Spinner mRepeatModeSpinner;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private CastListener mCastListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private RemoteMediaPlayer mMediaPlayer;
    private boolean mShouldPlayMedia;
    private MediaInfo mSelectedMedia;
    private ApplicationMetadata mAppMetadata;
    private boolean mSeeking;
    private boolean mWaitingForReconnect;
    private TextView mStartTimeTitle;
    private TextView mStartTimeTextView;
    private boolean mResolvingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStartTimeTitle = (TextView) findViewById(R.id.start_time_title);
        mStartTimeTitle.setVisibility(View.VISIBLE);
        mStartTimeTextView = (TextView) findViewById(R.id.start_time_text_view);
        mStartTimeTextView.setVisibility(View.VISIBLE);
        mStartTimeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialog = new EditTextDialog(R.string.enter_start_time) {
                    @Override
                    protected void onDialogPositiveClick(String text) {
                        mStartTimeTextView.setText(text);
                    }
                };
                dialog.show(getSupportFragmentManager(), START_TIME_DIALOG_TAG);
            }
        });

        mLoadQueueButton = (Button) findViewById(R.id.load_queue_button);
        mLoadQueueButton.setVisibility(View.VISIBLE);
        mLoadQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SdkCastPlayerActivity.this, QueueLoadActivity.class);
                putExtraMediaStatus(mMediaPlayer.getMediaStatus(), intent);
                startActivityForResult(intent, RESULT_CODE_QUEUE_LOAD);
            }
        });
        mInsertQueueButton = (Button) findViewById(R.id.insert_queue_button);
        mInsertQueueButton.setVisibility(View.VISIBLE);
        mInsertQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =
                        new Intent(SdkCastPlayerActivity.this, QueueInsertItemsActivity.class);
                putExtraMediaStatus(mMediaPlayer.getMediaStatus(), intent);
                startActivityForResult(intent, RESULT_CODE_QUEUE_INSERT);
            }
        });
        mUpdateQueueButton = (Button) findViewById(R.id.update_queue_button);
        mUpdateQueueButton.setVisibility(View.VISIBLE);
        mUpdateQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SdkCastPlayerActivity.this,
                        QueueUpdateItemsActivity.class);
                putExtraMediaStatus(mMediaPlayer.getMediaStatus(), intent);
                startActivityForResult(intent, RESULT_CODE_QUEUE_UPDATE);
            }
        });
        mRemoveQueueButton = (Button) findViewById(R.id.remove_queue_button);
        mRemoveQueueButton.setVisibility(View.VISIBLE);
        mRemoveQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SdkCastPlayerActivity.this,
                        QueueRemoveItemsActivity.class);
                putExtraMediaStatus(mMediaPlayer.getMediaStatus(), intent);
                startActivityForResult(intent, RESULT_CODE_QUEUE_REMOVE);
            }
        });
        mQueuePrevButton = (Button) findViewById(R.id.queue_prev_button);
        mQueuePrevButton.setVisibility(View.VISIBLE);
        mQueuePrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayer.queuePrev(mApiClient, null).setResultCallback(
                        new MediaResultCallback(getString(R.string.mediaop_queue_prev)));
            }
        });
        mQueueNextButton = (Button) findViewById(R.id.queue_next_button);
        mQueueNextButton.setVisibility(View.VISIBLE);
        mQueueNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayer.queueNext(mApiClient, null).setResultCallback(
                        new MediaResultCallback(getString(R.string.mediaop_queue_next)));
            }
        });
        mQueueJumpButton = (Button) findViewById(R.id.queue_jump_button);
        mQueueJumpButton.setVisibility(View.VISIBLE);
        mQueueJumpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SdkCastPlayerActivity.this, QueueJumpActivity.class);
                putExtraMediaStatus(mMediaPlayer.getMediaStatus(), intent);
                startActivityForResult(intent, RESULT_CODE_QUEUE_JUMP);
            }
        });
        mQueueInsertAndPlayItemButton =
                (Button) findViewById(R.id.queue_insert_and_play_item_button);
        mQueueInsertAndPlayItemButton.setVisibility(View.VISIBLE);
        mQueueInsertAndPlayItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SdkCastPlayerActivity.this,
                        QueueInsertAndPlayItemActivity.class);
                putExtraMediaStatus(mMediaPlayer.getMediaStatus(), intent);
                startActivityForResult(intent, RESULT_CODE_QUEUE_INSERT_AND_PLAY);
            }
        });
        mRepeatModeSpinner = (Spinner) findViewById(R.id.repeat_mode_spinner);
        mRepeatModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                    long id) {
                if (mMediaPlayer != null) {
                    MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
                    if ((mediaStatus == null) || (mediaStatus.getQueueRepeatMode() != position)) {
                        mMediaPlayer.queueSetRepeatMode(mApiClient, position, null)
                                .setResultCallback(new MediaResultCallback(
                                        getString(R.string.mediaop_set_repeat_mode)));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // do nothing.
            }
        });

        setCurrentDeviceName(null);
        setAutoplayCheckboxVisible(true);
        setStreamVolumeControlsVisible(true);
        setSeekBehaviorControlsVisible(true);
        setAppControlsVisible(true);
        setSessionControlsVisible(false);
        mConnectionCallbacks = new ConnectionCallbacks();
        mConnectionFailedListener = new ConnectionFailedListener();

        mCastListener = new CastListener();
        mLoadMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playMedia(mSelectedMedia);
            }
        });
    }

    private void putExtraMediaStatus(final MediaStatus mediaStatus, Intent intent) {
        if (mediaStatus == null) {
            return;
        }
        intent.putExtra(QueueLoadActivity.EXTRA_REPEAT_MODE, mediaStatus.getQueueRepeatMode());
        int count = mediaStatus.getQueueItemCount();
        String[] itemStrings = new String[count];
        for (int i = 0; i < count; ++i) {
            itemStrings[i] = mediaStatus.getQueueItem(i).toJson().toString();
        }
        intent.putExtra(QueueLoadActivity.EXTRA_ITEMS, itemStrings);
        intent.putExtra(QueueJumpActivity.EXTRA_CURRENT_ITEM_ID, mediaStatus.getCurrentItemId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case RESULT_CODE_QUEUE_LOAD: {
                String[] jsonItems = data.getStringArrayExtra(QueueLoadActivity.EXTRA_ITEMS);
                MediaQueueItem[] items = new MediaQueueItem[jsonItems.length];
                for (int i = 0; i < jsonItems.length; ++i) {
                    try {
                        items[i] = new MediaQueueItem.Builder(new JSONObject(jsonItems[i])).build();
                    } catch (JSONException e) {
                        // should never happen.
                    }
                }
                int repeatMode = data.getIntExtra(QueueLoadActivity.EXTRA_REPEAT_MODE,
                        MediaStatus.REPEAT_MODE_REPEAT_ALL);
                int startIndex = data.getIntExtra(QueueLoadActivity.EXTRA_START_INDEX, 0);
                if (data.hasExtra(QueueJumpActivity.EXTRA_START_TIME)) {
                    double startTime = data.getDoubleExtra(QueueJumpActivity.EXTRA_START_TIME, 0);
                    mMediaPlayer.queueLoad(mApiClient, items, startIndex, repeatMode,
                            secondsToMillis(startTime), null)
                            .setResultCallback(new MediaResultCallback(
                                    getString(R.string.mediaop_queue_load)));
                } else {
                    mMediaPlayer.queueLoad(mApiClient, items, startIndex, repeatMode, null)
                            .setResultCallback(new MediaResultCallback(
                                    getString(R.string.mediaop_queue_load)));
                }
                break;
            }
            case RESULT_CODE_QUEUE_INSERT: {
                String[] jsonItems = data.getStringArrayExtra(QueueInsertItemsActivity.EXTRA_ITEMS);
                int insertBeforeItemId =
                        data.getIntExtra(QueueInsertItemsActivity.EXTRA_INSERT_BEFORE_ITEM_ID,
                                MediaQueueItem.INVALID_ITEM_ID);
                MediaQueueItem[] items = new MediaQueueItem[jsonItems.length];
                for (int i = 0; i < jsonItems.length; ++i) {
                    try {
                        items[i] = new MediaQueueItem.Builder(new JSONObject(jsonItems[i])).build();
                    } catch (JSONException e) {
                        // should never happen.
                    }
                }
                mMediaPlayer.queueInsertItems(mApiClient, items, insertBeforeItemId, null)
                        .setResultCallback(new MediaResultCallback(
                                getString(R.string.mediaop_queue_insert)));
                break;
            }
            case RESULT_CODE_QUEUE_UPDATE: {
                String[] jsonItems = data.getStringArrayExtra(QueueUpdateItemsActivity.EXTRA_ITEMS);
                MediaQueueItem[] items = new MediaQueueItem[jsonItems.length];
                for (int i = 0; i < jsonItems.length; ++i) {
                    try {
                        items[i] = new MediaQueueItem.Builder(new JSONObject(jsonItems[i])).build();
                    } catch (JSONException e) {
                        // should never happen.
                    }
                }
                mMediaPlayer.queueUpdateItems(mApiClient, items, null)
                        .setResultCallback(new MediaResultCallback(
                                getString(R.string.mediaop_queue_update)));
                break;
            }
            case RESULT_CODE_QUEUE_REMOVE: {
                int[] itemIdsToRemove =
                        data.getIntArrayExtra(QueueRemoveItemsActivity.EXTRA_ITEM_IDS);
                mMediaPlayer.queueRemoveItems(mApiClient, itemIdsToRemove, null)
                        .setResultCallback(new MediaResultCallback(
                                getString(R.string.mediaop_queue_remove)));
                break;
            }
            case RESULT_CODE_QUEUE_JUMP: {
                int currentItemId = data.getIntExtra(QueueJumpActivity.EXTRA_CURRENT_ITEM_ID, 0);
                if (data.hasExtra(QueueJumpActivity.EXTRA_START_TIME)) {
                    double startTime = data.getDoubleExtra(QueueJumpActivity.EXTRA_START_TIME, 0);
                    mMediaPlayer.queueJumpToItem(mApiClient, currentItemId,
                            secondsToMillis(startTime), null)
                            .setResultCallback(new MediaResultCallback(
                                    getString(R.string.mediaop_queue_jump)));
                } else {
                    mMediaPlayer.queueJumpToItem(mApiClient, currentItemId, null)
                            .setResultCallback(new MediaResultCallback(
                                    getString(R.string.mediaop_queue_jump)));
                }
                break;
            }
            case RESULT_CODE_QUEUE_INSERT_AND_PLAY: {
                String jsonItem =
                        data.getStringExtra(QueueInsertAndPlayItemActivity.EXTRA_INSERTED_ITEM);
                MediaQueueItem item = null;
                try {
                    item = new MediaQueueItem.Builder(new JSONObject(jsonItem)).build();
                } catch (JSONException e) {
                    // should never happen.
                }
                int insertBeforeItemId =
                        data.getIntExtra(QueueInsertItemsActivity.EXTRA_INSERT_BEFORE_ITEM_ID,
                                MediaQueueItem.INVALID_ITEM_ID);
                if (data.hasExtra(QueueJumpActivity.EXTRA_START_TIME)) {
                    double startTime = data.getDoubleExtra(QueueJumpActivity.EXTRA_START_TIME, 0);
                    mMediaPlayer.queueInsertAndPlayItem(mApiClient, item, insertBeforeItemId,
                            secondsToMillis(startTime), null)
                            .setResultCallback(new MediaResultCallback(
                                    getString(R.string.mediaop_queue_insert_and_play_item)));
                } else {
                    mMediaPlayer.queueInsertAndPlayItem(mApiClient, item, insertBeforeItemId, null)
                            .setResultCallback(new MediaResultCallback(
                                    getString(R.string.mediaop_queue_insert_and_play_item)));
                }
                break;
            }
            case REQUEST_RESOLVE_ERROR: {
                mResolvingError = false;
                if (resultCode == RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if ((mApiClient != null) && !mApiClient.isConnecting()
                            && !mApiClient.isConnected()) {
                        mApiClient.connect();
                    } else {
                        setSelectedDevice(null);
                    }
                } else {
                    setSelectedDevice(null);
                }
                break;
            }
            default:
                Log.i(TAG, "Unknown request code: " + requestCode);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateButtonStates();
        // Restart the timer, if there's a media connection.
        if ((mMediaPlayer != null) && !mWaitingForReconnect) {
            startRefreshTimer();
        }
    }

    @Override
    protected void onVolumeChange(double delta) {
        if ((mApiClient == null) || !mApiClient.isConnected()) {
            return;
        }

        try {
            double volume = Cast.CastApi.getVolume(mApiClient) + delta;
            refreshDeviceVolume(volume, Cast.CastApi.isMute(mApiClient));
            Cast.CastApi.setVolume(mApiClient, volume);
        } catch (IOException e) {
            Log.w(TAG, "Unable to change volume", e);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Unable to change volume", e);
        }
    }

    @Override
    protected void onMediaSelected(final MediaInfo media) {
        mSelectedMedia = media;
        mLoadMediaButton.setEnabled(media != null);
        if (media == null) {
            return;
        }
        setCurrentMediaTracks(media.getMediaTracks());
    }

    /*
     * Connects to the device (if necessary), and then casts the currently selected video.
     */
    @Override
    protected void onPlayMedia(final MediaInfo media) {
        if (mAppMetadata == null) {
            return;
        }

        playMedia(mSelectedMedia);
    }

    @Override
    protected void onLaunchAppClicked() {
        if (!mApiClient.isConnected()) {
            return;
        }

        Cast.CastApi.launchApplication(mApiClient, getReceiverApplicationId(), getRelaunchApp())
                .setResultCallback(new ApplicationConnectionResultCallback("LaunchApp"));
    }

    @Override
    protected void onJoinAppClicked() {
        if (!mApiClient.isConnected()) {
            return;
        }

        Cast.CastApi.joinApplication(mApiClient, getReceiverApplicationId()).setResultCallback(
                new ApplicationConnectionResultCallback("JoinApplication"));
    }

    @Override
    protected void onLeaveAppClicked() {
        if (!mApiClient.isConnected()) {
            return;
        }

        Cast.CastApi.leaveApplication(mApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status result) {
                if (result.isSuccess()) {
                    mAppMetadata = null;
                    detachMediaPlayer();
                    updateButtonStates();
                } else {
                    showErrorDialog(getString(R.string.error_app_leave_failed));
                }
            }
        });
    }

    @Override
    protected void onStopAppClicked() {
        if (!mApiClient.isConnected()) {
            return;
        }

        Cast.CastApi.stopApplication(mApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status result) {
                if (result.isSuccess()) {
                    mAppMetadata = null;
                    detachMediaPlayer();
                    updateButtonStates();
                } else {
                    showErrorDialog(getString(R.string.error_app_stop_failed));
                }
            }
        });
    }

    @Override
    protected void onPlayClicked() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.play(mApiClient).setResultCallback(
                new MediaResultCallback(getString(R.string.mediaop_play)));
    }

    @Override
    protected void onPauseClicked() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.pause(mApiClient).setResultCallback(
                new MediaResultCallback(getString(R.string.mediaop_pause)));
    }

    @Override
    protected void onStopClicked() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.stop(mApiClient).setResultCallback(
                new MediaResultCallback(getString(R.string.mediaop_stop)));

    }

    @Override
    protected void onSeekBarMoved(long position) {
        if (mMediaPlayer == null) {
            return;
        }

        refreshPlaybackPosition(position, -1);

        int behavior = getSeekBehavior();

        int resumeState;
        switch (behavior) {
            case AFTER_SEEK_PLAY:
                resumeState = RemoteMediaPlayer.RESUME_STATE_PLAY;
                break;
            case AFTER_SEEK_PAUSE:
                resumeState = RemoteMediaPlayer.RESUME_STATE_PAUSE;
                break;
            case AFTER_SEEK_DO_NOTHING:
            default:
                resumeState = RemoteMediaPlayer.RESUME_STATE_UNCHANGED;
        }
        mSeeking = true;
        mMediaPlayer.seek(mApiClient, position, resumeState).setResultCallback(
                new MediaResultCallback(getString(R.string.mediaop_seek)) {
                    @Override
                    protected void onFinished() {
                        mSeeking = false;
                    }
                });
    }

    @Override
    protected void onDeviceVolumeBarMoved(int volume) {
        if (!mApiClient.isConnected()) {
            return;
        }
        try {
            Cast.CastApi.setVolume(mApiClient, volume / MAX_VOLUME_LEVEL);
        } catch (IOException e) {
            Log.w(TAG, "Unable to change volume");
        } catch (IllegalStateException e) {
            showErrorDialog(e.getMessage());
        }
    }

    @Override
    protected void onDeviceMuteToggled(boolean on) {
        if (!mApiClient.isConnected()) {
            return;
        }
        try {
            Cast.CastApi.setMute(mApiClient, on);
        } catch (IOException e) {
            Log.w(TAG, "Unable to toggle mute");
        } catch (IllegalStateException e) {
            showErrorDialog(e.getMessage());
        }
    }

    @Override
    protected void onStreamVolumeBarMoved(int volume) {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            mMediaPlayer.setStreamVolume(mApiClient, volume / MAX_VOLUME_LEVEL).setResultCallback(
                    new MediaResultCallback(getString(R.string.mediaop_set_stream_volume)));
        } catch (IllegalStateException e) {
            showErrorDialog(e.getMessage());
        }
    }

    @Override
    protected void onStreamMuteToggled(boolean on) {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            mMediaPlayer.setStreamMute(mApiClient, on).setResultCallback(
                    new MediaResultCallback(getString(R.string.mediaop_toggle_stream_mute)));
        } catch (IllegalStateException e) {
            showErrorDialog(e.getMessage());
        }
    }

    @Override
    protected void onMediaTracksSelected(long[] trackIds) {
        if (mMediaPlayer == null) {
            return;
        }
        setSelectedMediaTracks(trackIds);
        if (mMediaPlayer.getMediaStatus() == null) {
            return;
        }
        try {
            mMediaPlayer.setActiveMediaTracks(mApiClient, trackIds).setResultCallback(
                    new MediaResultCallback(getString(R.string.mediaop_set_active_media_tracks)));
        } catch (IllegalStateException e) {
            showErrorDialog(e.getMessage());
        }
    }

    @Override
    protected void onTextTrackStyleUpdated() {
        if (mMediaPlayer == null) {
            return;
        }
        MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
        if (mediaStatus == null) {
            return;
        }

        Log.d(TAG, "updating text track style");
        try {
            mMediaPlayer.setTextTrackStyle(mApiClient, getTextTrackStyle()).setResultCallback(
                    new MediaResultCallback(getString(R.string.mediaop_set_text_track_style)));
        } catch (IllegalStateException e) {
            showErrorDialog(e.getMessage());
        }
    }

    private static long secondsToMillis(double sec) {
        return (long) (sec * DateUtils.SECOND_IN_MILLIS);
    }

    private void clearMediaState() {
        setCurrentMediaMetadata(null, null, null);
        setCurrentMediaTracks(null);
        mLoadMediaButton.setEnabled(false);
        refreshPlaybackPosition(0, 0);
    }

    private void attachMediaPlayer() {
        if (mMediaPlayer != null) {
            return;
        }

        mMediaPlayer = new RemoteMediaPlayer();
        mMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {

            @Override
            public void onStatusUpdated() {
                Log.d(TAG, "MediaControlChannel.onStatusUpdated");
                // If item has ended, clear metadata.
                MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
                if ((mediaStatus != null)
                        && (mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE)) {
                    clearMediaState();
                }

                updatePlaybackPosition();
                updateActiveTracks();
                updateStreamVolume();
                updateButtonStates();
            }
        });

        mMediaPlayer.setOnMetadataUpdatedListener(
                new RemoteMediaPlayer.OnMetadataUpdatedListener() {
            @Override
            public void onMetadataUpdated() {
                Log.d(TAG, "MediaControlChannel.onMetadataUpdated");
                String title = null;
                String artist = null;
                Uri imageUrl = null;

                MediaInfo mediaInfo = mMediaPlayer.getMediaInfo();
                if (mediaInfo != null) {
                    MediaMetadata metadata = mediaInfo.getMetadata();
                    if (metadata != null) {
                        title = metadata.getString(MediaMetadata.KEY_TITLE);

                        artist = metadata.getString(MediaMetadata.KEY_ARTIST);
                        if (artist == null) {
                            artist = metadata.getString(MediaMetadata.KEY_STUDIO);
                        }

                        List<WebImage> images = metadata.getImages();
                        if ((images != null) && !images.isEmpty()) {
                            WebImage image = images.get(0);
                            imageUrl = image.getUrl();
                        }
                    }
                    setCurrentMediaMetadata(title, artist, imageUrl);
                    setCurrentMediaTracks(mediaInfo.getMediaTracks());
                    mLoadMediaButton.setEnabled(false);
                    setSeekBarEnabled(mediaInfo.getStreamType() == MediaInfo.STREAM_TYPE_BUFFERED);
                }
            }
        });

        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mMediaPlayer.getNamespace(),
                    mMediaPlayer);
        } catch (IOException e) {
            Log.w(TAG, "Exception while launching application", e);
        }
    }

    private void requestMediaStatus() {
        if (mMediaPlayer == null) {
            return;
        }

        Log.d(TAG, "requesting current media status");
        mMediaPlayer.requestStatus(mApiClient).setResultCallback(
                new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                    @Override
                    public void onResult(MediaChannelResult result) {
                        Status status = result.getStatus();
                        if (!status.isSuccess()) {
                            Log.w(TAG, "Unable to request status: " + status.getStatusCode());
                        }
                    }
                });
    }

    private void detachMediaPlayer() {
        if ((mMediaPlayer != null) && (mApiClient != null)) {
            try {
                Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
                        mMediaPlayer.getNamespace());
            } catch (IOException e) {
                Log.w(TAG, "Exception while detaching media player", e);
            }
        }
        mMediaPlayer = null;
    }

    /*
     * Begins playback of the currently selected video.
     */
    private void playMedia(MediaInfo media) {
        Log.d(TAG, "playMedia: " + media);
        if (media == null) {
            return;
        }
        if (mMediaPlayer == null) {
            Log.e(TAG, "Trying to play a video with no active media session");
            return;
        }

        media.setTextTrackStyle(getTextTrackStyle());

        mMediaPlayer.load(mApiClient, media, isAutoplayChecked(),
                secondsToMillis(Double.valueOf(mStartTimeTextView.getText().toString())),
                getSelectedMediaTracks(), null)
                .setResultCallback(new MediaResultCallback(getString(R.string.mediaop_load)));
    }

    private void updateButtonStates() {
        boolean hasDeviceConnection = (mApiClient != null) && mApiClient.isConnected()
                && !mWaitingForReconnect;
        boolean hasAppConnection = (mAppMetadata != null) && !mWaitingForReconnect;
        boolean hasMediaConnection = (mMediaPlayer != null) && !mWaitingForReconnect;
        boolean hasMedia = false;
        boolean seekableStream = false;

        if (hasMediaConnection) {
            MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
            if (mediaStatus != null) {
                int mediaPlayerState = mediaStatus.getPlayerState();
                int playerState = PLAYER_STATE_NONE;
                if (mediaPlayerState == MediaStatus.PLAYER_STATE_PAUSED) {
                    playerState = PLAYER_STATE_PAUSED;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_PLAYING) {
                    playerState = PLAYER_STATE_PLAYING;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_BUFFERING) {
                    playerState = PLAYER_STATE_BUFFERING;
                }
                setPlayerState(playerState);

                hasMedia = mediaStatus.getPlayerState() != MediaStatus.PLAYER_STATE_IDLE;
                MediaInfo mediaInfo = mediaStatus.getMediaInfo();
                if (mediaInfo != null) {
                    seekableStream = mediaInfo.getStreamType() == MediaInfo.STREAM_TYPE_BUFFERED;
                }
                setRepeatModeState(mediaStatus.getQueueRepeatMode());
            }
        } else {
            setPlayerState(PLAYER_STATE_NONE);
            setRepeatModeState(MediaStatus.REPEAT_MODE_REPEAT_OFF);
        }


        mLaunchAppButton.setEnabled(hasDeviceConnection && !hasAppConnection);
        mJoinAppButton.setEnabled(hasDeviceConnection && !hasAppConnection);
        mLeaveAppButton.setEnabled(hasDeviceConnection && hasAppConnection);
        mStopAppButton.setEnabled(hasDeviceConnection && hasAppConnection);
        mAutoplayCheckbox.setEnabled(hasDeviceConnection && hasAppConnection);
        mStartTimeTitle.setEnabled(hasDeviceConnection && hasAppConnection);
        mStartTimeTextView.setEnabled(hasDeviceConnection && hasAppConnection);

        mSelectMediaButton.setEnabled(hasMediaConnection);
        mStopButton.setEnabled(hasMediaConnection && hasMedia);
        setSeekBarEnabled(hasMediaConnection && hasMedia && seekableStream);
        setDeviceVolumeControlsEnabled(hasDeviceConnection);
        setStreamVolumeControlsEnabled(hasMediaConnection && hasMedia);

        mLoadQueueButton.setEnabled(hasMediaConnection);
        mInsertQueueButton.setEnabled(hasMedia);
        mQueueJumpButton.setEnabled(hasMedia);
        mUpdateQueueButton.setEnabled(hasMedia);
        mRemoveQueueButton.setEnabled(hasMedia);
        mQueuePrevButton.setEnabled(hasMedia);
        mQueueNextButton.setEnabled(hasMedia);
        mQueueInsertAndPlayItemButton.setEnabled(hasMedia);
    }

    private void setRepeatModeState(int repeat_mode) {
        mRepeatModeSpinner.setSelection(repeat_mode);
    }

    private void updatePlaybackPosition() {
        if (mMediaPlayer == null) {
            return;
        }
        refreshPlaybackPosition(mMediaPlayer.getApproximateStreamPosition(),
                mMediaPlayer.getStreamDuration());
    }

    private void updateActiveTracks() {
        long[] selectedTracks = null;

        MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
        if (mediaStatus != null) {
            selectedTracks = mediaStatus.getActiveTrackIds();
        }
        setSelectedMediaTracks(selectedTracks);
    }

    private void updateStreamVolume() {
        if (mMediaPlayer == null) {
            return;
        }
        MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
        if (mediaStatus != null) {
            double streamVolume = mediaStatus.getStreamVolume();
            boolean muteState = mediaStatus.isMute();
            refreshStreamVolume(streamVolume, muteState);
        }
    }

    private void setSelectedDevice(CastDevice device) {
        mSelectedDevice = device;
        setCurrentDeviceName(mSelectedDevice != null ? mSelectedDevice.getFriendlyName() : null);

        if (mSelectedDevice == null) {
            detachMediaPlayer();
            setStandbyState(Cast.STANDBY_STATE_NO);
            setActiveInputState(Cast.ACTIVE_INPUT_STATE_NO);
            if ((mApiClient != null) && mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
        } else {
            setStandbyState(Cast.STANDBY_STATE_UNKNOWN);
            setActiveInputState(Cast.ACTIVE_INPUT_STATE_UNKNOWN);
            Log.d(TAG, "acquiring controller for " + mSelectedDevice);
            try {
                Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(
                        mSelectedDevice, mCastListener);
                apiOptionsBuilder.setVerboseLoggingEnabled(true);
                mApiClient = new GoogleApiClient.Builder(this)
                        .addApi(Cast.API, apiOptionsBuilder.build())
                        .addConnectionCallbacks(mConnectionCallbacks)
                        .addOnConnectionFailedListener(mConnectionFailedListener)
                        .build();
                mApiClient.connect();
            } catch (IllegalStateException e) {
                Log.w(TAG, "error while creating a device controller", e);
                showErrorDialog(getString(R.string.error_no_controller));
            }
        }
    }

    @Override
    protected String getControlCategory() {
        return CastMediaControlIntent.categoryForCast(getReceiverApplicationId());
    }

    @Override
    protected void onRouteSelected(RouteInfo route) {
        Log.d(TAG, "onRouteSelected: " + route);

        CastDevice device = CastDevice.getFromBundle(route.getExtras());
        setSelectedDevice(device);
        updateButtonStates();
    }

    @Override
    protected void onRouteUnselected(RouteInfo route) {
        Log.d(TAG, "onRouteUnselected: " + route);
        setSelectedDevice(null);
        mAppMetadata = null;
        clearMediaState();
        updateButtonStates();
    }

    @Override
    protected void onRefreshEvent() {
        if (!mSeeking) {
            updatePlaybackPosition();
        }
        updateStreamVolume();
        updateButtonStates();
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO: need to disable all controls, and possibly display a
                    // "reconnecting..." dialog or overlay
                    mWaitingForReconnect = true;
                    cancelRefreshTimer();
                    detachMediaPlayer();
                    updateButtonStates();
                }
            });
        }

        @Override
        public void onConnected(final Bundle connectionHint) {
            Log.d(TAG, "ConnectionCallbacks.onConnected");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mApiClient.isConnected()) {
                        // We got disconnected while this runnable was pending execution.
                        return;
                    }
                    try {
                        Cast.CastApi.requestStatus(mApiClient);
                    } catch (IOException e) {
                        Log.d(TAG, "error requesting status", e);
                    }
                    setDeviceVolumeControlsEnabled(true);
                    mLaunchAppButton.setEnabled(true);
                    mJoinAppButton.setEnabled(true);

                    if (mWaitingForReconnect) {
                        mWaitingForReconnect = false;
                        if ((connectionHint != null)
                                && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                            Log.d(TAG, "App  is no longer running");
                            detachMediaPlayer();
                            mAppMetadata = null;
                            clearMediaState();
                            updateButtonStates();
                        } else {
                            attachMediaPlayer();
                            requestMediaStatus();
                            startRefreshTimer();
                        }
                    }
                }
            });
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(final ConnectionResult result) {
            Log.d(TAG, "onConnectionFailed");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateButtonStates();
                    clearMediaState();
                    cancelRefreshTimer();
                    if (mResolvingError) {
                        Log.d(TAG, "Already attempting to resolve an error");
                        return;
                    } else if (result.hasResolution()) {
                        try {
                            mResolvingError = true;
                            result.startResolutionForResult(SdkCastPlayerActivity.this,
                                    REQUEST_RESOLVE_ERROR);
                        } catch (SendIntentException e) {
                            if (mApiClient != null) {
                                mApiClient.connect();
                            }
                        }
                    } else {
                        // Show dialog using GooglePlayServicesUtil.getErrorDialog()
                        showErrorDialog(getString(R.string.error_no_device_connection,
                                result.getErrorCode(),
                                CastStatusCodes.getStatusCodeString(result.getErrorCode())));
                        setSelectedDevice(null);
                        if (!mMediaRouter.getSelectedRoute().equals(
                                mMediaRouter.getDefaultRoute())) {
                            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
                        }
                        mAppMetadata = null;
                    }
                }
            });
        }
    }

    private class CastListener extends Cast.Listener {
        @Override
        public void onVolumeChanged() {
            try {
                refreshDeviceVolume(Cast.CastApi.getVolume(mApiClient),
                        Cast.CastApi.isMute(mApiClient));
            } catch (IllegalStateException e) {
                Log.w(TAG, "Unable to refresh the volume", e);
            }
        }

        @Override
        public void onApplicationStatusChanged() {
            try {
                String status = Cast.CastApi.getApplicationStatus(mApiClient);
                Log.d(TAG, "onApplicationStatusChanged; status=" + status);
                setApplicationStatus(status);
            } catch (IllegalStateException e) {
                Log.w(TAG, "Unable to get the application status", e);
            }
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d(TAG, "onApplicationDisconnected: statusCode=" + statusCode);
            mAppMetadata = null;
            detachMediaPlayer();
            clearMediaState();
            updateButtonStates();
            if (statusCode != CastStatusCodes.SUCCESS) {
                // This is an unexpected disconnect.
                setApplicationStatus(getString(R.string.status_app_disconnected));
            }
        }

        @Override
        public void onActiveInputStateChanged(int activeInputState) {
            setActiveInputState(activeInputState);
        }

        @Override
        public void onStandbyStateChanged(int standbyState) {
            setStandbyState(standbyState);
        }
    }

    private final class ApplicationConnectionResultCallback implements
            ResultCallback<Cast.ApplicationConnectionResult> {
        private final String mClassTag;

        public ApplicationConnectionResultCallback(String suffix) {
            mClassTag = TAG + "_" + suffix;
        }

        @Override
        public void onResult(ApplicationConnectionResult result) {
            Status status = result.getStatus();
            Log.d(mClassTag, "ApplicationConnectionResultCallback.onResult: statusCode"
                    + status.getStatusCode());
            if (status.isSuccess()) {
                ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                String sessionId = result.getSessionId();
                String applicationStatus = result.getApplicationStatus();
                boolean wasLaunched = result.getWasLaunched();
                Log.d(mClassTag, "application name: " + applicationMetadata.getName()
                        + ", status: " + applicationStatus + ", sessionId: " + sessionId
                        + ", wasLaunched: " + wasLaunched);
                setApplicationStatus(applicationStatus);
                attachMediaPlayer();
                mAppMetadata = applicationMetadata;
                startRefreshTimer();
                updateButtonStates();
                Log.d(mClassTag, "mShouldPlayMedia is " + mShouldPlayMedia);
                if (mShouldPlayMedia) {
                    mShouldPlayMedia = false;
                    Log.d(mClassTag, "now loading media");
                    playMedia(mSelectedMedia);
                } else {
                    // Synchronize with the receiver's state.
                    requestMediaStatus();
                }
            } else {
                showErrorDialog(getString(R.string.error_app_launch_failed));
            }
        }
    }

    private class MediaResultCallback implements ResultCallback<MediaChannelResult> {
        private final String mOperationName;

        public MediaResultCallback(String operationName) {
            mOperationName = operationName;
        }

        @Override
        public void onResult(MediaChannelResult result) {
            Status status = result.getStatus();
            // Ignore STATUS_REPLACED since it's just an informative status and doesn't indicate
            // a failure.
            if (!status.isSuccess()
                    && (status.getStatusCode() != RemoteMediaPlayer.STATUS_REPLACED)) {
                Log.w(TAG, mOperationName + " failed: " + status.getStatusCode());
                showErrorDialog(getString(R.string.error_operation_failed, mOperationName));
            } else {
                Toast.makeText(getApplicationContext(), mOperationName + " done",
                        Toast.LENGTH_SHORT).show();
            }
            onFinished();
        }

        protected void onFinished() {
        }
    }

}
