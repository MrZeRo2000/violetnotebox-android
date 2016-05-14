package com.romanpulov.violetnotebox;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dropbox.core.DbxException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

public class MainActivity extends AppCompatActivity {

    private void log(String message) {
        Log.d("MainActivity", message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Log.d("MainActivity", "Starting auth");
        //Auth.startOAuth2Authentication(MainActivity.this, getString(R.string.app_key));
        /*
        String accessToken = Auth.getOAuth2Token();
        Log.d("MainActivity", "accessToken=" + accessToken);
        DropboxClientFactory.init(accessToken);
        Log.d("MainActivity", "Client init");
        */
        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Auth.startOAuth2Authentication(MainActivity.this, getString(R.string.app_key));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("dropbox-sample", MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            log("no access token, getting");
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString("access-token", accessToken).apply();
                initAndLoadData(accessToken);
            }
        } else {
            log("access token available, loading data");
            initAndLoadData(accessToken);
        }
    }

    private void initAndLoadData(String accessToken) {
        log("init client");
        DropboxClientFactory.init(accessToken);
        log("load data");
        loadData();
    }

    private static class FileListAsyncTask extends AsyncTask<String, Void, ListFolderResult> {
        private final DbxClientV2 mClient;
        private final Callback mCallback;
        private Exception mException;

        public interface Callback {
            void onDataLoaded(ListFolderResult result);
            void onError(Exception e);
        }


        @Override
        protected ListFolderResult doInBackground(String... params) {
            try {
                return mClient.files().listFolder(params[0]);
            } catch (DbxException e) {
                mException = e;
            }
            return null;
        }

        public FileListAsyncTask(DbxClientV2 client, Callback callback) {
            mClient = client;
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(ListFolderResult listFolderResult) {
            super.onPostExecute(listFolderResult);

            if (mException != null) {
                mCallback.onError(mException);
            } else {
                mCallback.onDataLoaded(listFolderResult);
            }
        }
    }

    protected void loadData() {
        FileListAsyncTask task = new FileListAsyncTask(DropboxClientFactory.getClient(),
                new FileListAsyncTask.Callback() {
                    @Override
                    public void onDataLoaded(ListFolderResult result) {
                        log("onDataLoaded:");
                        for (Metadata p: result.getEntries()) {
                            log(p.getName());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        log("onError:" + e.getMessage());
                    }
                }
        );
        task.execute("/Temp");


        /*

        String accessToken = Auth.getOAuth2Token();
        if (accessToken == null) {
            Log.d("MainActivity", "access token is null, authenticating");
            Auth.startOAuth2Authentication(MainActivity.this, getString(R.string.app_key));
        } else {
            Log.d("MainActivity", "access token not null");
            DropboxClientFactory.init(accessToken);
            Log.d("MainActivity", "client factory init");
            Log.d("MainActivity", "Listing files");
            try {
                ListFolderResult r = DropboxClientFactory.getClient().files().listFolder("/Temp/*.*");
                for (Metadata p : r.getEntries()) {
                    Log.d("MainActivity", p.getName());
                }
            } catch (DbxException e) {
                e.printStackTrace();
            }
        }
        */
    }
}
