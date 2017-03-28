package com.google.android.gms.drive.sample.realtimeplayground;

import com.google.android.gms.drive.realtime.CollaborativeList;
import com.google.android.gms.drive.realtime.CollaborativeMap;
import com.google.android.gms.drive.realtime.CollaborativeMap.ValueChangedEvent;
import com.google.android.gms.drive.realtime.Collaborator;
import com.google.android.gms.drive.realtime.CompoundOperation;
import com.google.android.gms.drive.realtime.IndexReference;
import com.google.android.gms.drive.realtime.IndexReference.ReferenceShiftedEvent;
import com.google.android.gms.drive.realtime.Model;
import com.google.android.gms.drive.realtime.RealtimeDocument;
import com.google.android.gms.drive.realtime.RealtimeEvent;
import com.google.android.gms.drive.realtime.RealtimeEvent.Listener;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A fragment that displays the sample CollaborativeList.
 */
public class CollaborativeListFragment extends PlaygroundFragment {
    private static final String TAG = "CollaborativeListFragment";

    private RealtimeDocument mRealtimeDocument;
    private CollaborativeList mCollaborativeList;
    private CollaborativeMap mCursors;
    private IndexReference mCollaboratorCursor;
    private String mMeSessionId;

    private ArrayAdapter<Object> mItemsArrayAdapter;

    private RealtimeEvent.Listener<CollaborativeList.ValuesSetEvent> mValuesSetListener;
    private RealtimeEvent.Listener<CollaborativeList.ValuesAddedEvent> mValuesAddedListener;
    private RealtimeEvent.Listener<CollaborativeList.ValuesRemovedEvent> mValuesRemovedListener;

    private Listener<ValueChangedEvent> mCursorValueChangedListener;
    private RealtimeEvent.Listener<RealtimeDocument.CollaboratorLeftEvent> mLeftListener;

    private ListView mItemsList;
    private EditText mAddItemEditText;
    private EditText mSetItemEditText;
    private EditText mNewIndexEditText;

    /**
     * Tracks the session id -> listener listening for changes.
     */
    private Map<String, Listener<ReferenceShiftedEvent>> mCursorEvents;
    /**
     * Tracks for each row, the set of collaborators that have it currently selected.
     */
    private Map<Integer, Set<Collaborator>> mRowToCollaborators;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_collaborative_list, container, false);
        Button removeButton = (Button) view.findViewById(R.id.remove_item_button);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doRemoveSelected();
            }
        });

        Button addButton = (Button) view.findViewById(R.id.add_item_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doAddItem();
            }
        });

        Button clearButton = (Button) view.findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doClearList();
            }
        });

        Button setButton = (Button) view.findViewById(R.id.set_item_button);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSetItem();
            }
        });

        Button moveButton = (Button) view.findViewById(R.id.move_button);
        moveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doMoveItem();
            }
        });

        mAddItemEditText = (EditText) view.findViewById(R.id.add_item_edit_text);
        mSetItemEditText = (EditText) view.findViewById(R.id.set_item_edit_text);
        mNewIndexEditText = (EditText) view.findViewById(R.id.new_index_edit_text);
        mItemsList = (ListView) view.findViewById(R.id.items_list);

        mItemsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                doItemSelected(i);
            }
        });

        disableViewUntilLoaded(removeButton);
        disableViewUntilLoaded(addButton);
        disableViewUntilLoaded(clearButton);
        disableViewUntilLoaded(setButton);
        disableViewUntilLoaded(moveButton);
        disableViewUntilLoaded(mAddItemEditText);
        disableViewUntilLoaded(mSetItemEditText);
        disableViewUntilLoaded(mNewIndexEditText);
        disableViewUntilLoaded(mItemsList);

        return view;
    }

    private void doRemoveSelected() {
        if (mItemsList.getCheckedItemPosition() != AdapterView.INVALID_POSITION) {
            Log.d(TAG, "removing " + mItemsList.getCheckedItemPosition());
            mCollaborativeList.remove(mItemsList.getCheckedItemPosition());
        }
    }

    private void doAddItem() {
        if (mAddItemEditText.length() > 0) {
            mCollaborativeList.add(mAddItemEditText.getText().toString());
            mAddItemEditText.setText("");
        }
    }

    private void doClearList() {
        mCollaborativeList.clear();
    }

    private void doSetItem() {
        if (mItemsList.getCheckedItemPosition() != AdapterView.INVALID_POSITION
                && mSetItemEditText.length() > 0) {
            mCollaborativeList.set(
                    mItemsList.getCheckedItemPosition(), mSetItemEditText.getText().toString());
        }
    }

    private void doMoveItem() {
        if (mItemsList.getCheckedItemPosition() != AdapterView.INVALID_POSITION) {
            try {
                int newIndex = Integer.parseInt(mNewIndexEditText.getText().toString());
                if (newIndex >= 0 && newIndex <= mCollaborativeList.size()) {
                    mCollaborativeList.move(mItemsList.getCheckedItemPosition(), newIndex);
                } else {
                    Toast.makeText(getActivity(), "Invalid index.", Toast.LENGTH_LONG).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Invalid index value.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void doItemSelected(final Integer index) {
        CompoundOperation cursorOperation = new CompoundOperation() {
            @Override
            public void performCompoundOperation(Model model) {
                if (index!= null) {
                    if (mCollaboratorCursor == null) {
                        mCollaboratorCursor = mCollaborativeList.registerReference(index, true);
                        mCursors.put(mMeSessionId, mCollaboratorCursor);
                    } else {
                        mCollaboratorCursor.setIndex(index);
                    }
                } else {
                    mCursors.remove(mMeSessionId);
                }
            }
        };

        mRealtimeDocument.getModel().performCompoundOperation(
                cursorOperation, "CursorOperation", false);
    }

    @Override
    public String getTitle() {
        return "Collaborative List";
    }

    @Override
    void onLoaded(RealtimeDocument document) {
        mRealtimeDocument = document;

        mLeftListener = new RealtimeEvent.Listener<RealtimeDocument.CollaboratorLeftEvent>() {
            @Override
            public void onEvent(RealtimeDocument.CollaboratorLeftEvent event) {
                stopListeningForCollaboratorCursorEvents(
                        event.getCollaborator().getSessionId(), event.getCollaborator());
            }
        };

        mRealtimeDocument.addCollaboratorLeftListener(mLeftListener);
        mCollaborativeList =
                (CollaborativeList) mRealtimeDocument.getModel().getRoot().get(
                        PlaygroundDocumentActivity.COLLAB_LIST_NAME);
        mRowToCollaborators = new HashMap<>();
        mItemsArrayAdapter = new ItemsArrayAdapter();
        mItemsList.setAdapter(mItemsArrayAdapter);
        mValuesAddedListener = new RealtimeEvent.Listener<CollaborativeList.ValuesAddedEvent>() {
            @Override
            public void onEvent(CollaborativeList.ValuesAddedEvent event) {
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        };
        mCollaborativeList.addValuesAddedListener(mValuesAddedListener);
        mValuesRemovedListener =
                new RealtimeEvent.Listener<CollaborativeList.ValuesRemovedEvent>() {
                    @Override
                    public void onEvent(CollaborativeList.ValuesRemovedEvent event) {
                        mItemsArrayAdapter.notifyDataSetChanged();
                    }
                };
        mCollaborativeList.addValuesRemovedListener(mValuesRemovedListener);
        mValuesSetListener = new RealtimeEvent.Listener<CollaborativeList.ValuesSetEvent>() {
            @Override
            public void onEvent(CollaborativeList.ValuesSetEvent event) {
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        };
        mCollaborativeList.addValuesSetListener(mValuesSetListener);

        mCursors = (CollaborativeMap) document.getModel().getRoot().get(
                PlaygroundDocumentActivity.CURSORS_NAME);
        initCollaboratorCursors();
    }

    /**
     * Initializing tracking of collaborator cursors.
     *
     * <p>Adds an IndexReference for the "me" collaborator if it doesn't exist yet that is updated
     * when the collaborator selects an item in the list.
     *
     * <p>Adds listeners for any existing collaborators and updates the UI to show the row they
     * have selected.
     */
    private void initCollaboratorCursors() {
        // find the me session
        for (Collaborator collaborator : mRealtimeDocument.getCollaborators()) {
            if (collaborator.isMe()) {
                mMeSessionId = collaborator.getSessionId();
                break;
            }
        }

        mCursorEvents = new HashMap<>();
        // add a listener for each active collaborator that isn't me.
        for (final Collaborator collaborator : mRealtimeDocument.getCollaborators()) {
            if (collaborator.isMe()) {
                continue;
            }
            listenForCollaboratorCursorEvents(collaborator.getSessionId());
            buildCursorMapForExistingCollaborators(collaborator);
        }

        // Rerender in case of any existing collaborators on the doc
        mItemsArrayAdapter.notifyDataSetChanged();

        // listen for cursors added or removed from the list
        mCursorValueChangedListener = new Listener<ValueChangedEvent>() {
            @Override
            public void onEvent(ValueChangedEvent event) {
                if (event.getNewValue() != null) {
                    listenForCollaboratorCursorEvents(event.getProperty());
                } else {
                    stopListeningForCollaboratorCursorEvents(event.getProperty());
                }
            }
        };
        mCursors.addValueChangedListener(mCursorValueChangedListener);
    }

    private void listenForCollaboratorCursorEvents(String sessionId) {
        final Collaborator collaborator = getCollaborator(sessionId);
        if (collaborator == null) {
            // collaborator not active.
            return;
        }
        IndexReference ref = (IndexReference) mCursors.get(collaborator.getSessionId());
        if (ref == null) {
            return;
        }
        Listener<ReferenceShiftedEvent> listener = new Listener<ReferenceShiftedEvent>() {
            @Override
            public void onEvent(ReferenceShiftedEvent event) {
                Set<Collaborator> oldRow = mRowToCollaborators.get(event.getOldIndex());
                if (oldRow != null) {
                    oldRow.remove(collaborator);
                }
                Set<Collaborator> newRow = mRowToCollaborators.get(event.getNewIndex());
                if (newRow == null) {
                    newRow = new HashSet<>();
                    mRowToCollaborators.put(event.getNewIndex(), newRow);
                }
                newRow.add(collaborator);
                // notify the adapter that something changed, so it rerenders
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        };
        ref.addReferenceShiftedListener(listener);
        mCursorEvents.put(collaborator.getSessionId(), listener);
    }

    private void buildCursorMapForExistingCollaborators(Collaborator collaborator) {
        if (collaborator == null) {
            return;
        }
        IndexReference ref = (IndexReference) mCursors.get(collaborator.getSessionId());
        if (ref == null) {
            return;
        }
        Set<Collaborator> row = new HashSet<>();
        mRowToCollaborators.put(ref.getIndex(), row);
        row.add(collaborator);
    }

    private Collaborator getCollaborator(String sessionId) {
        for (Collaborator c : mRealtimeDocument.getCollaborators()) {
            if (c.getSessionId().equals(sessionId)) {
                return c;
            }
        }
        return null;
    }

    private void stopListeningForCollaboratorCursorEvents(String sessionId) {
        Listener<ReferenceShiftedEvent> listener = mCursorEvents.get(sessionId);
        IndexReference reference = (IndexReference) mCursors.get(sessionId);
        if (listener != null && reference != null) {
            reference.removeReferenceShiftedListener(listener);
        }
        mCursorEvents.remove(sessionId);
    }

    // Removes collaborator from row and notifies the adapter of a change
    private void stopListeningForCollaboratorCursorEvents(
            String sessionId, Collaborator collaborator){
        Listener<ReferenceShiftedEvent> listener = mCursorEvents.get(sessionId);
        IndexReference reference = (IndexReference) mCursors.get(sessionId);
        if (listener != null && reference != null) {
            mRowToCollaborators.get(reference.getIndex()).remove(collaborator);
            reference.removeReferenceShiftedListener(listener);
            mItemsArrayAdapter.notifyDataSetChanged();
        }
        mCursorEvents.remove(sessionId);
    }

    private class ItemsArrayAdapter extends ArrayAdapter<Object> {
        public ItemsArrayAdapter() {
            super(
                    getActivity(),
                    android.R.layout.simple_list_item_single_choice,
                    mCollaborativeList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            Set<Collaborator> collaborators = mRowToCollaborators.get(position);
            if (collaborators != null && collaborators.size() > 0) {
                view.setBackgroundColor(collaborators.iterator().next().getColor());
            } else {
                view.setBackgroundColor(0);
            }
            return view;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mValuesAddedListener != null) {
            mCollaborativeList.removeValuesAddedListener(mValuesAddedListener);
        }
        if (mValuesSetListener != null) {
            mCollaborativeList.removeValuesSetListener(mValuesSetListener);
        }
        if (mValuesRemovedListener != null) {
            mCollaborativeList.removeValuesRemovedListener(mValuesRemovedListener);
        }
        if (mCursorValueChangedListener != null) {
            mCursors.removeValueChangedListener(mCursorValueChangedListener);
        }
        if (mLeftListener != null) {
            mRealtimeDocument.removeCollaboratorLeftListener(mLeftListener);
        }
    }
}
