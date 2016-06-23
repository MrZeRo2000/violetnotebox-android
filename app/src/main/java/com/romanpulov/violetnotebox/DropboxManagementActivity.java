package com.romanpulov.violetnotebox;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
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

import com.dropbox.core.DbxDownloader;
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
    private static final String LOCAL_FILE_NAME = "localfile";

    private static final int STATUS_OK = 0;
    private static final int STATUS_ERROR = 1;

    private SharedPreferences mPrefs;
    private String mAccessToken;

    private TextView mAccessTokenTextView;
    private TextView mMessageTextView;

    private interface DropboxCallback {
        void onDataLoaded(FileMetadata fileMetadata);
        void onError(Exception e);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox_management);

        mPrefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        mAccessToken = getAccessToken();
        log("AccessToken=" + mAccessToken);

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

        /*
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
        */

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

        mMessageTextView = (TextView)findViewById(R.id.messageTextView);

        Button syncButton = (Button)findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String remotePath = mPrefs.getString(SHARED_PREFERENCES_REMOTE_PATH, null);
                String remoteFileName = mPrefs.getString(SHARED_PREFERENCES_REMOTE_FILE_NAME, null);
                log("sync: " + remotePath + ", " + remoteFileName);

                mAccessToken = Auth.getOAuth2Token();
                if (mAccessToken == null) {
                    setMessage(STATUS_ERROR, getResources().getString(R.string.message_error_auth));
                    return;
                }

                log("setting access token:" + mAccessToken);
                setAccessToken(mAccessToken);
                updateAccessTokenTextView();

                log("init with access token:" + mAccessToken);
                DropboxClientFactory.init(mAccessToken);

                File f = new File(getCacheDir(), LOCAL_FILE_NAME);
                FileGetAsyncTask fileGetAsyncTask = new FileGetAsyncTask(DropboxClientFactory.getClient(), f, new DropboxCallback() {
                    @Override
                    public void onDataLoaded(FileMetadata fileMetadata) {
                        if (fileMetadata.getSize() > 0)
                            setMessage(STATUS_OK, fileMetadata.getName());
                        else
                            setMessage(STATUS_ERROR, getResources().getString(R.string.message_error_download_file_zero));
                    }

                    @Override
                    public void onError(Exception e) {
                        setMessage(STATUS_ERROR, e.getMessage());
                    }
                });
                fileGetAsyncTask.execute(remotePath, remoteFileName);
            }
        });

    }

    private void setMessage(int status, String message) {
        int messageColor;
        String messageText;
        switch (status) {
            case STATUS_ERROR:
                messageColor = getResources().getColor(R.color.colorTextError);
                messageText = getResources().getString(R.string.message_error_loading, message);
                break;
            case STATUS_OK:
                messageColor = getResources().getColor(R.color.colorTextMessage);
                messageText = getResources().getString(R.string.message_successfully_loaded, message);
                break;
            default:
                messageColor = 0;
                messageText = null;
        }

        mMessageTextView.setTextColor(messageColor);
        mMessageTextView.setText(messageText);
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

    private class FileGetAsyncTask extends AsyncTask<String, Void, FileMetadata> {
        private ProgressDialog mProgressDialog;
        private final DbxClientV2 mClient;
        private final File mFile;
        private final DropboxCallback mCallback;
        private Exception mException;

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(DropboxManagementActivity.this);
            mProgressDialog.setTitle(getResources().getString(R.string.message_downloading_file_progress));
            mProgressDialog.show();
        }

        @Override
        protected FileMetadata doInBackground(String... params) {
            try {
                ListFolderResult folderResult = mClient.files().listFolder(params[0]);
                for (Metadata e: folderResult.getEntries()) {
                    if ((e instanceof FileMetadata) && (e.getName().equals(params[1]))) {
                        FileMetadata f = (FileMetadata) e;
                        OutputStream outputStream = new FileOutputStream(mFile);
                        try  {
                            return mClient.files().download(e.getPathLower(), f.getRev()).download(outputStream);
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
        protected void onPostExecute(FileMetadata fileMetadata) {
            super.onPostExecute(fileMetadata);
            mProgressDialog.dismiss();

            if (mException != null) {
                mCallback.onError(mException);
            } else {
                mCallback.onDataLoaded(fileMetadata);
            }
        }
    }


}
