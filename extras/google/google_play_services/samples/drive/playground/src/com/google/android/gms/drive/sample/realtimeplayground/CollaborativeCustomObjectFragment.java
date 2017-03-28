// Copyright 2014 Google Inc. All Rights Reserved.

package com.google.android.gms.drive.sample.realtimeplayground;

import com.google.android.gms.drive.realtime.CustomCollaborativeObject;
import com.google.android.gms.drive.realtime.CustomCollaborativeObject.FieldChangedEvent;
import com.google.android.gms.drive.realtime.RealtimeDocument;
import com.google.android.gms.drive.realtime.RealtimeEvent.Listener;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class CollaborativeCustomObjectFragment extends PlaygroundFragment {
    private RealtimeDocument mRealtimeDocument;
    private Movie mCustomMovie;

    private EditText mTitleEditText;
    private EditText mDirectorNameEditText;
    private EditText mNotesEditText;
    private EditText mStarRatingEditText;

    /**
     * Tracks whether changes to the TextView should be ignored so that we don't attempt to update
     * the data model for changes that are the result of remote mutations or undo/redo.
     */
    private boolean mIgnoreTextViewChanges = false;

    /**
     * Tracks whether changes to the data model should be ignored so that we don't attempt to update
     * the TextView for changes that are a result of user action.
     */
    private boolean mIgnoreDataModelChanges = false;

    private Listener<FieldChangedEvent> mFieldChangedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(
                R.layout.fragment_collaborative_custom_object, container, false);

        mTitleEditText = (EditText) view.findViewById(R.id.movie_title_edit_text);
        mDirectorNameEditText = (EditText) view.findViewById(R.id.movie_director_name_edit_text);
        mNotesEditText = (EditText) view.findViewById(R.id.movie_notes_edit_text);
        mStarRatingEditText = (EditText) view.findViewById(R.id.movie_star_rating_edit_text);

        disableViewUntilLoaded(mTitleEditText);
        disableViewUntilLoaded(mDirectorNameEditText);
        disableViewUntilLoaded(mNotesEditText);
        disableViewUntilLoaded(mStarRatingEditText);

        return view;
    }

    /**
     * Add TextWatcher to EditTexts so changes automatically update the Movie object. This is used
     * instead of a CollaborativeString, to keep the custom object simple, or a save button to
     * update in realtime. It is, however, slow because it's saving after each character.
     */
    private void setEditTextTextWatcher(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                mIgnoreDataModelChanges = true;
                if (!mIgnoreTextViewChanges) {
                    if (mCustomMovie != null && editText.getText() != null) {
                        /*
                         * The TextWatcher is sometimes called multiple times for a single change,
                         * so we need to compare the current Movie field to the "new" text.
                         */
                        String currText = editText.getText().toString();
                        switch (editText.getId()) {
                            case R.id.movie_title_edit_text:
                                if (mCustomMovie.getTitle() == null
                                        || !mCustomMovie.getTitle().equals(currText)) {
                                    mCustomMovie.setTitle(editable.toString());
                                }
                                break;
                            case R.id.movie_director_name_edit_text:
                                if (mCustomMovie.getDirectorName() == null
                                        || !mCustomMovie.getDirectorName().equals(currText)) {
                                    mCustomMovie.setDirectorName(editable.toString());
                                }
                                break;
                            case R.id.movie_notes_edit_text:
                                if (mCustomMovie.getNotes() == null
                                        || !mCustomMovie.getNotes().equals(currText)) {
                                    mCustomMovie.setNotes(editable.toString());
                                }
                                break;
                            case R.id.movie_star_rating_edit_text:
                                if (mCustomMovie.getStarRating() == null
                                        || !mCustomMovie.getStarRating().equals(currText)) {
                                    mCustomMovie.setStarRating(editable.toString());
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                editText.setSelection(editable.length());
                mIgnoreDataModelChanges = false;
            }
        });
    }

    @Override
    String getTitle() {
        return "Collaborative Custom Object";
    }

    @Override
    void onLoaded(RealtimeDocument document) {
        mRealtimeDocument = document;
        mCustomMovie = new Movie((CustomCollaborativeObject) mRealtimeDocument.getModel().getRoot()
                .get(PlaygroundDocumentActivity.COLLAB_CUSTOM_OBJ_NAME));
        mTitleEditText.setText(mCustomMovie.getTitle());
        mDirectorNameEditText.setText(mCustomMovie.getDirectorName());
        mNotesEditText.setText(mCustomMovie.getNotes());
        mStarRatingEditText.setText(mCustomMovie.getStarRating());

        mFieldChangedListener = new Listener<FieldChangedEvent>() {
            @Override
            public void onEvent(FieldChangedEvent event) {
                if (mIgnoreDataModelChanges) {
                    return;
                }
                mIgnoreTextViewChanges = true;

                try {
                    String fieldName = event.getFieldName();
                    if (fieldName.equals("name")) {
                        mTitleEditText.setText((String) event.getNewFieldValue());
                    } else if (fieldName.equals("director")) {
                        mDirectorNameEditText.setText((String) event.getNewFieldValue());
                    } else if (fieldName.equals("notes")) {
                        mNotesEditText.setText((String) event.getNewFieldValue());
                    } else if (fieldName.equals("rating")) {
                        mStarRatingEditText.setText((String) event.getNewFieldValue());
                    }
                } finally {
                    mIgnoreTextViewChanges = false;
                }
            }
        };

        mCustomMovie.addTitleChangedListener(mFieldChangedListener);
        mCustomMovie.addDirectorNameChangedListener(mFieldChangedListener);
        mCustomMovie.addNotesChangedListener(mFieldChangedListener);
        mCustomMovie.addStarRatingChangedListener(mFieldChangedListener);

        setEditTextTextWatcher(mTitleEditText);
        setEditTextTextWatcher(mDirectorNameEditText);
        setEditTextTextWatcher(mNotesEditText);
        setEditTextTextWatcher(mStarRatingEditText);
    }

    public static class Movie {
        private CustomCollaborativeObject mMovie;

        Movie(CustomCollaborativeObject obj) {
            this.mMovie = obj;
        }

        CustomCollaborativeObject getCustomObject() {
            return mMovie;
        }

        String getTitle() {
            return (String) mMovie.get("name");
        }

        void setTitle(String title) {
            mMovie.set("name", title);
        }

        void addTitleChangedListener(Listener<FieldChangedEvent> listener) {
            mMovie.addFieldChangedListener(listener);
        }

        String getDirectorName() {
            return (String) mMovie.get("director");
        }

        void setDirectorName(String directorName) {
            mMovie.set("director", directorName);
        }

        void addDirectorNameChangedListener(Listener<FieldChangedEvent> listener) {
            mMovie.addFieldChangedListener(listener);
        }

        String getNotes() {
            return (String) mMovie.get("notes");
        }

        void setNotes(String notes) {
            mMovie.set("notes", notes);
        }

        void addNotesChangedListener(Listener<FieldChangedEvent> listener) {
            mMovie.addFieldChangedListener(listener);
        }

        String getStarRating() {
            return (String) mMovie.get("rating");
        }

        void setStarRating(String rating) {
            mMovie.set("rating", rating);
        }

        void addStarRatingChangedListener(Listener<FieldChangedEvent> listener) {
            mMovie.addFieldChangedListener(listener);
        }
    }
}
