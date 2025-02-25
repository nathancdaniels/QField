/**
 * QFieldActivity.java - class needed to copy files from assets to
 * getExternalFilesDir() before starting QtActivity this can be used to perform
 * actions before QtActivity takes over.
 * @author  Marco Bernasocchi - <marco@opengis.ch>
 * @version 0.5
 */
/*
 Copyright (c) 2011, Marco Bernasocchi <marco@opengis.ch>
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the  Marco Bernasocchi <marco@opengis.ch> nor the
 names of its contributors may be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY Marco Bernasocchi <marco@opengis.ch> ''AS IS'' AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL Marco Bernasocchi <marco@opengis.ch> BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch.opengis.qfield;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import ch.opengis.qfield.QFieldUtils;
import ch.opengis.qfield.R;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.qtproject.qt5.android.bindings.QtActivity;

public class QFieldActivity extends QtActivity {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferenceEditor;

    public static native void openProject(String url);
    private float originalBrightness;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        prepareQtActivity();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() == Intent.ACTION_VIEW) {
            Uri uri = intent.getData();
            Context context = getApplication().getApplicationContext();
            openProject(QFieldUtils.getPathFromUri(context, uri));
        }
    }

    private void dimBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        originalBrightness = lp.screenBrightness;
        lp.screenBrightness = 0.01f;
        getWindow().setAttributes(lp);
    }

    private void restoreBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = originalBrightness;
        getWindow().setAttributes(lp);
    }

    private void prepareQtActivity() {
        sharedPreferences =
            getSharedPreferences("QField", Context.MODE_PRIVATE);
        sharedPreferenceEditor = sharedPreferences.edit();

        checkPermissions();
        checkAllFileAccess(); // Storage access permission handling for Android
                              // 11+

        String storagePath =
            Environment.getExternalStorageDirectory().getAbsolutePath();

        String qFieldDir = storagePath + "/QField/";
        new File(qFieldDir).mkdir();

        // create directories
        new File(qFieldDir + "basemaps/").mkdir();
        new File(qFieldDir + "fonts/").mkdir();
        new File(qFieldDir + "proj/").mkdir();
        new File(qFieldDir + "auth/").mkdir();

        Intent intent = new Intent();
        intent.setClass(QFieldActivity.this, QtActivity.class);
        try {
            ActivityInfo activityInfo = getPackageManager().getActivityInfo(
                getComponentName(), PackageManager.GET_META_DATA);
            intent.putExtra("GIT_REV", activityInfo.metaData.getString(
                                           "android.app.git_rev"));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            finish();
            return;
        }

        intent.putExtra("QFIELD_DATA_DIR", qFieldDir);

        Intent sourceIntent = getIntent();
        if (sourceIntent.getAction() == Intent.ACTION_VIEW) {
            Uri uri = sourceIntent.getData();
            Context context = getApplication().getApplicationContext();
            intent.putExtra("QGS_PROJECT",
                            QFieldUtils.getPathFromUri(context, uri));
        }
        setIntent(intent);
    }

    private void checkPermissions() {
        List<String> permissionsList = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(
                QFieldActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_DENIED) {
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(
                QFieldActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_DENIED) {
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(
                QFieldActivity.this,
                Manifest.permission.ACCESS_MEDIA_LOCATION) ==
            PackageManager.PERMISSION_DENIED) {
            permissionsList.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(QFieldActivity.this,
                                              Manifest.permission.BLUETOOTH) ==
            PackageManager.PERMISSION_DENIED) {
            permissionsList.add(Manifest.permission.BLUETOOTH);
        }
        if (ContextCompat.checkSelfPermission(
                QFieldActivity.this, Manifest.permission.BLUETOOTH_ADMIN) ==
            PackageManager.PERMISSION_DENIED) {
            permissionsList.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (permissionsList.size() > 0) {
            String[] permissions = new String[permissionsList.size()];
            permissionsList.toArray(permissions);
            ActivityCompat.requestPermissions(QFieldActivity.this, permissions,
                                              101);
        }
    }

    private void checkAllFileAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager() &&
            !sharedPreferences.getBoolean("DontAskAllFilesPermission", false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.grant_permission));
            builder.setMessage(
                Html.fromHtml(getString(R.string.grant_all_files_permission),
                              Html.FROM_HTML_MODE_LEGACY));
            builder.setPositiveButton(
                getString(R.string.grant),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            Intent intent = new Intent(
                                Settings
                                    .ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(
                                "QField",
                                "Failed to initial activity to grant all files access",
                                e);
                        }
                        dialog.dismiss();
                    }
                });
            builder.setNegativeButton(
                getString(R.string.deny_always),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        sharedPreferenceEditor.putBoolean(
                            "DontAskAllFilesPermission", true);
                        sharedPreferenceEditor.commit();

                        dialog.dismiss();
                    }
                });

            builder.setNeutralButton(getString(R.string.deny_once),
                                     new DialogInterface.OnClickListener() {
                                         public void onClick(
                                             DialogInterface dialog, int id) {
                                             dialog.dismiss();
                                         }
                                     });

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();
        }
    }
}
