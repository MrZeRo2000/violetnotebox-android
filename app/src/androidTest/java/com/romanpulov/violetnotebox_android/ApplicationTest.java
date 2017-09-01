package com.romanpulov.violetnotebox_android;

import android.app.Application;
import android.os.Environment;
import android.test.ApplicationTestCase;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testListBackupFileNames() {
        String folderName = Environment.getExternalStorageDirectory().toString() + "/" + "VioletNoteBackup" + "/";
        Log.d("testListBackupFileNames", "FolderName:" + folderName);

        File ff = new File(folderName);
        File[] files = ff.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String lowFileName = pathname.getAbsolutePath().toLowerCase();
                boolean result = lowFileName.endsWith("zip");
                if (!result) {
                    int zipIndex = lowFileName.lastIndexOf("zip");
                    if (zipIndex > -1) {
                        if (lowFileName.matches("\\S*zip.bak[0-9]{2}"))
                            result = true;
                        /*
                        String zipFileNamePart = lowFileName.substring(zipIndex, zipIndex + 7);
                        if (zipFileNamePart.equals("zip.bak"))
                            result = true;
                            */
                    }
                }

                return result;
            }
        });

        for (File f : files) {
            Log.d("testListBackupFileNames", f.getAbsolutePath() + "(" + f.getName() + ")");
        }
    }
}