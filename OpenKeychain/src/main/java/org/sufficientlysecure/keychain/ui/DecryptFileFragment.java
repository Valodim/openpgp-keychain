/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.util.Notify;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;

public class DecryptFileFragment extends DecryptFragment {
    public static final String ARG_FILENAME = "filename";

    private static final int RESULT_CODE_FILE = 0x00007003;

    // view
    private EditText mFilename;
    private CheckBox mDeleteAfter;
    private ImageButton mBrowse;
    private View mDecryptButton;

    private String mInputFilename = null;
    private Uri mInputUri = null;
    private String mOutputFilename = null;
    private Uri mOutputUri = null;

    private FileDialogFragment mFileDialog;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_file_fragment, container, false);

        mFilename = (EditText) view.findViewById(R.id.decrypt_file_filename);
        mBrowse = (ImageButton) view.findViewById(R.id.decrypt_file_browse);
        mDeleteAfter = (CheckBox) view.findViewById(R.id.decrypt_file_delete_after_decryption);
        mDecryptButton = view.findViewById(R.id.decrypt_file_action_decrypt);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Constants.KITKAT) {
                    FileHelper.openDocument(DecryptFileFragment.this, mInputUri, "*/*", RESULT_CODE_FILE);
                } else {
                    FileHelper.openFile(DecryptFileFragment.this, mFilename.getText().toString(), "*/*",
                            RESULT_CODE_FILE);
                }
            }
        });
        mDecryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAction();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String filename = getArguments().getString(ARG_FILENAME);
        if (filename != null) {
            mFilename.setText(filename);
        }
    }

    private String guessOutputFilename() {
        File file = new File(mInputFilename);
        String filename = file.getName();
        if (filename.endsWith(".asc") || filename.endsWith(".gpg") || filename.endsWith(".pgp")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        return Constants.Path.APP_DIR + "/" + filename;
    }

    private void decryptAction() {
        String currentFilename = mFilename.getText().toString();
        if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
            mInputUri = null;
            mInputFilename = mFilename.getText().toString();
        }

        if (mInputUri == null) {
            mOutputFilename = guessOutputFilename();
        }

        if (mInputFilename.equals("")) {
            //AppMsg.makeText(getActivity(), R.string.no_file_selected, AppMsg.STYLE_ALERT).show();
            Notify.showNotify(getActivity(), R.string.no_file_selected, Notify.Style.ERROR);
            return;
        }

        if (mInputUri == null && mInputFilename.startsWith("file")) {
            File file = new File(mInputFilename);
            if (!file.exists() || !file.isFile()) {
                AppMsg.makeText(
                        getActivity(),
                        getString(R.string.error_message,
                                getString(R.string.error_file_not_found)), AppMsg.STYLE_ALERT)
                        .show();
                return;
            }
        }

        askForOutputFilename();
    }

    private void askForOutputFilename() {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    if (data.containsKey(FileDialogFragment.MESSAGE_DATA_URI)) {
                        mOutputUri = data.getParcelable(FileDialogFragment.MESSAGE_DATA_URI);
                    } else {
                        mOutputFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);
                    }
                    decryptStart(null);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        mFileDialog = FileDialogFragment.newInstance(messenger,
                getString(R.string.title_decrypt_to_file),
                getString(R.string.specify_file_to_decrypt_to), mOutputFilename, null);

        mFileDialog.show(getActivity().getSupportFragmentManager(), "fileDialog");
    }

    @Override
    protected void decryptStart(String passphrase) {
        Log.d(Constants.TAG, "decryptStart");

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();

        intent.setAction(KeychainIntentService.ACTION_DECRYPT_VERIFY);

        // data
        Log.d(Constants.TAG, "mInputFilename=" + mInputFilename + ", mOutputFilename="
                + mOutputFilename + ",mInputUri=" + mInputUri + ", mOutputUri="
                + mOutputUri);

        if (mInputUri != null) {
            data.putInt(KeychainIntentService.SOURCE, KeychainIntentService.IO_URI);
            data.putParcelable(KeychainIntentService.ENCRYPT_INPUT_URI, mInputUri);
        } else {
            data.putInt(KeychainIntentService.SOURCE, KeychainIntentService.IO_FILE);
            data.putString(KeychainIntentService.ENCRYPT_INPUT_FILE, mInputFilename);
        }

        if (mOutputUri != null) {
            data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_URI);
            data.putParcelable(KeychainIntentService.ENCRYPT_OUTPUT_URI, mOutputUri);
        } else {
            data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_FILE);
            data.putString(KeychainIntentService.ENCRYPT_OUTPUT_FILE, mOutputFilename);
        }

        data.putString(KeychainIntentService.DECRYPT_PASSPHRASE, passphrase);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_decrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    PgpDecryptVerifyResult decryptVerifyResult =
                            returnData.getParcelable(KeychainIntentService.RESULT_DECRYPT_VERIFY_RESULT);

                    if (PgpDecryptVerifyResult.KEY_PASSHRASE_NEEDED == decryptVerifyResult.getStatus()) {
                        showPassphraseDialog(decryptVerifyResult.getKeyIdPassphraseNeeded());
                    } else if (PgpDecryptVerifyResult.SYMMETRIC_PASSHRASE_NEEDED ==
                                    decryptVerifyResult.getStatus()) {
                        showPassphraseDialog(Constants.key.symmetric);
                    } else {
                        // display signature result in activity
                        onResult(decryptVerifyResult);

                        if (mDeleteAfter.isChecked()) {
                            // Create and show dialog to delete original file
                            DeleteFileDialogFragment deleteFileDialog;
                            if (mInputUri != null) {
                                deleteFileDialog = DeleteFileDialogFragment.newInstance(mInputUri);
                            } else {
                                deleteFileDialog = DeleteFileDialogFragment
                                        .newInstance(mInputFilename);
                            }
                            deleteFileDialog.show(getActivity().getSupportFragmentManager(), "deleteDialog");
                        }
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_CODE_FILE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (Constants.KITKAT) {
                        mInputUri = data.getData();
                        Cursor cursor = getActivity().getContentResolver().query(mInputUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null) {
                            if (cursor.moveToNext()) {
                                mInputFilename = cursor.getString(0);
                                mFilename.setText(mInputFilename);
                            }
                            cursor.close();
                        }
                    } else {
                        try {
                            String path = FileHelper.getPath(getActivity(), data.getData());
                            Log.d(Constants.TAG, "path=" + path);

                            mFilename.setText(path);
                        } catch (NullPointerException e) {
                            Log.e(Constants.TAG, "Nullpointer while retrieving path!");
                        }
                    }
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }
}
