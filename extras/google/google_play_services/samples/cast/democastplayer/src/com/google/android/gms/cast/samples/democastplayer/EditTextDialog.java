// Copyright 2015 Google Inc. All Rights Reserved.

package com.google.android.gms.cast.samples.democastplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public abstract class EditTextDialog extends DialogFragment {
    private final int mTitleResourceId;
    private EditText mEditText;

    public EditTextDialog(int titleResourceId) {
        mTitleResourceId = titleResourceId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.edit_text_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        mEditText = (EditText) view.findViewById(R.id.edit_text);
        mEditText.setHint(mTitleResourceId);
        mEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        return builder
                .setTitle(mTitleResourceId)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                onDialogPositiveClick(mEditText.getText().toString());
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                onDialogNegativeClick();
                            }
                        }
                )
                .create();
    }

    protected abstract void onDialogPositiveClick(String text);

    protected void onDialogNegativeClick() {}
}
