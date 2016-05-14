package com.romanpulov.violetnotebox;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DropboxManagementActivity extends AppCompatActivity {

    private void log(String message) {
        Log.d("DMActivity", message);
    }


    private static final String SHARED_PREFERENCES_NAME = "dropbox-preferences";
    private static final String SHARED_PREFERENCES_ACCESS_TOKEN = "access-token";
    private static final String SHARED_PREFERENCES_REMOTE_PATH = "remote-path";
    private static final String SHARED_PREFERENCES_REMOTE_FILE_NAME = "remote-file-name";

    private SharedPreferences mPrefs;
    private String mAccessToken;

    private TextView mAccessTokenTextView;

    private interface DropboxCallback {
        void onDataLoaded(File file);
        void onError(Exception e);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox_management);

        mPrefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mAccessToken = getAccessToken();

        initControls();
    }

    private void initControls() {
        mAccessTokenTextView = (TextView)findViewById(R.id.accessTokenTextView);
        updateAccessTokenTextView();

        Button authenticateButton = (Button)findViewById(R.id.authenticateButton);
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Auth.startOAuth2Authentication(DropboxManagementActivity.this, getResources().getString(R.string.app_key));
            }
        });

        Button connectButton = (Button)findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("starting getOAuth2Token");
                String accessToken = Auth.getOAuth2Token();
                log("setting access token:" + accessToken);
                setAccessToken(accessToken);
                updateAccessTokenTextView();
            }
        });

        EditText remotePathEditText = (EditText)findViewById(R.id.remotePathEditText);
        remotePathEditText.setText(mPrefs.getString(SHARED_PREFERENCES_REMOTE_PATH, null));
        remotePathEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.edit().putString(SHARED_PREFERENCES_REMOTE_PATH, s.toString()).apply();
            }
        });

        EditText remoteFileNameEditText = (EditText)findViewById(R.id.remoteFileNameEditText);
        remoteFileNameEditText.setText(mPrefs.getString(SHARED_PREFERENCES_REMOTE_FILE_NAME, null));
        remoteFileNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mPrefs.edit().putString(SHARED_PREFERENCES_REMOTE_FILE_NAME, s.toString()).apply();
            }
        });

        final TextView messageTextView = (TextView)findViewById(R.id.messageTextView);

        Button syncButton = (Button)findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String remotePath = mPrefs.getString(SHARED_PREFERENCES_REMOTE_PATH, null);
                String remoteFileName = mPrefs.getString(SHARED_PREFERENCES_REMOTE_FILE_NAME, null);
                log("sync: " + remotePath + ", " + remoteFileName);


                DropboxClientFactory.init(mAccessToken);

                File f = new File(getCacheDir(), "file1");
                FileGetAsyncTask fileGetAsyncTask = new FileGetAsyncTask(DropboxClientFactory.getClient(), f, new DropboxCallback() {
                    @Override
                    public void onDataLoaded(File file) {
                        messageTextView.setText("Successfully loaded " + file.getName());
                    }

                    @Override
                    public void onError(Exception e) {
                        messageTextView.setText(e.getMessage());
                    }
                });
                fileGetAsyncTask.execute(remotePath, remoteFileName);
            }
        });

    }

    private String getAccessToken() {
        return mPrefs.getString(SHARED_PREFERENCES_ACCESS_TOKEN, null);
    }

    private void setAccessToken(String accessToken) {
        mAccessToken = accessToken;
        mPrefs.edit().putString(SHARED_PREFERENCES_ACCESS_TOKEN, accessToken).apply();
    }

    private void updateAccessTokenTextView() {
        if (mAccessToken == null)
            mAccessTokenTextView.setText(R.string.access_token_empty);
        else
            mAccessTokenTextView.setText(R.string.access_token_exists);
    }

    private class FileGetAsyncTask extends AsyncTask<String, Void, File> {
        private ProgressDialog mProgressDialog;
        private final DbxClientV2 mClient;
        private final File mFile;
        private final DropboxCallback mCallback;
        private Exception mException;

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(DropboxManagementActivity.this);
            mProgressDialog.setTitle("Loading ...");
            mProgressDialog.show();
        }

        @Override
        protected File doInBackground(String... params) {
            try {
                //return mClient.files().listFolder(params[0]);
                ListFolderResult folderResult = mClient.files().listFolder(params[0]);
                for (Metadata e: folderResult.getEntries()) {
                    if ((e instanceof FileMetadata) && (e.getName().equals(params[1]))) {
                        FileMetadata f = (FileMetadata) e;
                        OutputStream outputStream = new FileOutputStream(mFile);
                        try  {
                            mClient.files().download(e.getPathLower(), f.getRev()).download(outputStream);
                            return mFile;
                        } finally {
                            outputStream.flush();
                            outputStream.close();
                        }
                    }
                }
                throw new Exception("File " + params[1] + " not found on server");

            } catch (Exception e) {
                mException = e;
            }
            return null;
        }

        public FileGetAsyncTask(DbxClientV2 client, File file, DropboxCallback callback) {
            mClient = client;
            mFile = file;
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            mProgressDialog.dismiss();

            if (mException != null) {
                mCallback.onError(mException);
            } else {
                mCallback.onDataLoaded(file);
            }
        }
    }


}
