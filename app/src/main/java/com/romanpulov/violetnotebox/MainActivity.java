package com.romanpulov.violetnotebox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dropbox.core.DbxException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

public class MainActivity extends AppCompatActivity {

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

    }
}
