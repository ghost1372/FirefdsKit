/*
 * Copyright (C) 2016 Mohamed Karami for XTouchWiz Project (Wanam@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sb.firefds.pie.firefdskit;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import de.robv.android.xposed.library.ui.TextViewPreference;
import eu.chainfire.libsuperuser.Shell;
import sb.firefds.pie.firefdskit.dialogs.CreditsDialog;
import sb.firefds.pie.firefdskit.dialogs.DVFSBlackListDialog;
import sb.firefds.pie.firefdskit.dialogs.RestoreDialog;
import sb.firefds.pie.firefdskit.dialogs.RestoreDialog.RestoreDialogListener;
import sb.firefds.pie.firefdskit.dialogs.SaveDialog;
import sb.firefds.pie.firefdskit.notifications.RebootNotification;
import sb.firefds.pie.firefdskit.utils.Utils;

import static sb.firefds.pie.firefdskit.utils.Constants.PREFS;

@SuppressWarnings("deprecation")
public class XTouchWizActivity extends Activity implements RestoreDialogListener {

    private static ProgressDialog mDialog;
    private static final String[] defaultSettings = new String[]{"enableCameraDuringCall",
            "disableNumberFormating", "disableSmsToMmsConversion", "isFirefdsKitFirstLaunch",
            "makeMeTooLegit", "disableTIMA"};

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }

        if (!Settings.System.canWrite(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }
    }

    public static void fixPermissions(Context context) {
        File sharedPrefsFolder = new File(context.getDataDir().getAbsolutePath() + "/shared_prefs");
        if (sharedPrefsFolder.exists()) {
            sharedPrefsFolder.setExecutable(true, false);
            sharedPrefsFolder.setReadable(true, false);
            File f = new File(sharedPrefsFolder.getAbsolutePath() + "/" + BuildConfig.APPLICATION_ID + ".xml");
            if (f.exists()) {
                f.setReadable(true, false);
                sharedPrefsFolder.setExecutable(true, false);
            }
        }
    }

    public static void fixAppPermissions() {
        File appFolder = new File(Environment.getDataDirectory(), "data/" + BuildConfig.APPLICATION_ID);
        appFolder.setExecutable(true, false);
        appFolder.setReadable(true, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        fixAppPermissions();
        verifyStoragePermissions(this);

        setContentView(R.layout.firefds_main);

        try {

            MainApplication.setWindowsSize(new Point());
            getWindowManager().getDefaultDisplay().getSize(MainApplication.getWindowsSize());

            if (savedInstanceState == null)
                getFragmentManager().beginTransaction().replace(R.id.prefs,
                        new SettingsFragment()).commit();


        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        try {
            if (!isFinishing()) {
                mDialog = new ProgressDialog(this);
                mDialog.setCancelable(false);
                mDialog.setMessage(getString(R.string.exiting_the_application_));
                mDialog.show();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        new QuitTask().execute(this);
    }

    @Override
    protected void onDestroy() {
        try {
            if (mDialog != null) {
                mDialog.dismiss();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private static class QuitTask extends AsyncTask<Activity, Void, Void> {
        private Activity mActivity = null;

        protected Void doInBackground(Activity... params) {

            try {
                mActivity = params[0];

                XCscFeaturesManager.applyCscFeatures(MainApplication.getSharedPreferences());
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                Utils.resetPermissions(mActivity);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                if (mActivity != null) {
                    mActivity.finish();
                }
            }
            super.onPostExecute(result);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_credits:
                new CreditsDialog().show(getFragmentManager(), "credits");
                break;
            case R.id.recommended_settings:
                ShowRecommendedSettingsDiag();
                break;
            case R.id.action_save:
                new SaveDialog().show(getFragmentManager(), "save");
                break;
            case R.id.action_restore:
                new RestoreDialog().show(getFragmentManager(), "restore");
                break;

            default:
                break;
        }
        return true;

    }

    public void ShowRecommendedSettingsDiag() {
        AlertDialog.Builder builder = new AlertDialog.Builder(XTouchWizActivity.this);
        builder.setCancelable(true)
                .setTitle(R.string.app_name)
                .setMessage(R.string.set_recommended_settings)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .setPositiveButton(R.string.apply, (dialog, which) -> restoreRecommendedSettings())
                .create()
                .show();
    }

    public void restoreRecommendedSettings() {

        MainApplication.getSharedPreferences().edit().clear().apply();
        PreferenceManager.setDefaultValues(this, R.xml.firefds_settings, false);

        Editor editor = MainApplication.getSharedPreferences().edit();

        for (String defaultSetting : defaultSettings) {
            editor.putBoolean(defaultSetting, true).apply();
        }

        editor.putInt("notificationSize", MainApplication.getWindowsSize().x).apply();

        fixPermissions(getApplicationContext());

        Toast.makeText(this, R.string.recommended_restored, Toast.LENGTH_SHORT).show();

        XCscFeaturesManager.applyCscFeatures(MainApplication.getSharedPreferences());

        RebootNotification.notify(this, 999, false);

        recreate();

    }

    @Override
    public void onRestoreDefaults() {

        MainApplication.getSharedPreferences().edit().clear().apply();
        PreferenceManager.setDefaultValues(this, R.xml.firefds_settings, false);

        Toast.makeText(this, R.string.defaults_restored, Toast.LENGTH_SHORT).show();

        MainApplication.getSharedPreferences()
                .edit()
                .putInt("notificationSize", MainApplication.getWindowsSize().x)
                .apply();

        fixPermissions(getApplicationContext());

        XCscFeaturesManager.applyCscFeatures(MainApplication.getSharedPreferences());

        recreate();

        RebootNotification.notify(this, 999, false);
    }

    @Override
    public void onRestoreBackup(final File backup) {
        new RestoreBackupTask(backup).execute();
    }

    class RestoreBackupTask extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDialog;
        private File backup;

        RestoreBackupTask(File backup) {
            this.backup = backup;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(XTouchWizActivity.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.restoring_backup));
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            ObjectInputStream input = null;
            try {
                input = new ObjectInputStream(new FileInputStream(backup));
                Editor prefEdit = MainApplication.getSharedPreferences().edit();
                prefEdit.clear();
                @SuppressWarnings("unchecked")
                Map<String, ?> entries = (Map<String, ?>) input.readObject();
                for (Entry<String, ?> entry : entries.entrySet()) {
                    Object v = entry.getValue();
                    String key = entry.getKey();

                    if (v instanceof Boolean)
                        prefEdit.putBoolean(key, (Boolean) v);
                    else if (v instanceof Float)
                        prefEdit.putFloat(key, (Float) v);
                    else if (v instanceof Integer)
                        prefEdit.putInt(key, (Integer) v);
                    else if (v instanceof Long)
                        prefEdit.putLong(key, (Long) v);
                    else if (v instanceof String)
                        prefEdit.putString(key, ((String) v));
                }
                prefEdit.apply();
                fixPermissions(getApplicationContext());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            SystemClock.sleep(1500);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Toast.makeText(XTouchWizActivity.this, R.string.backup_restored, Toast.LENGTH_SHORT).show();
            RebootNotification.notify(XTouchWizActivity.this, 999, false);
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        private static Context mContext;

        // Fields
        private List<String> changesMade;
        private static Resources res;
        private AlertDialog alertDialog;
        private static ProgressDialog mDialog;

        private static Runnable delayedRoot = new Runnable() {

            @Override
            public void run() {
                try {
                    if (mDialog != null) {
                        mDialog.dismiss();
                        Toast.makeText(mContext, R.string.root_info, Toast.LENGTH_LONG).show();
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }

            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            try {
                changesMade = new ArrayList<>();
                mContext = getActivity().getBaseContext();

                res = getResources();

                SharedPreferences sharedPreferences
                        = mContext.getSharedPreferences(PREFS, 0);
                MainApplication.setSharedPreferences(sharedPreferences);
                addPreferencesFromResource(R.xml.firefds_settings);

                showDiag();

                MainApplication.getSharedPreferences().edit()
                        .putInt("notificationSize", MainApplication.getWindowsSize().x).apply();
                fixPermissions(mContext);


                if (!Utils.isSamsungRom()) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setTitle(res.getString(R.string.samsung_rom_warning));

                    alertDialogBuilder.setMessage(res.getString(R.string.samsung_rom_warning_msg))
                            .setCancelable(false)
                            .setPositiveButton(res.getString(R.string.ok_btn), null);

                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }

                findPreference("disableDVFSWhiteList").setOnPreferenceClickListener(preference -> {
                    new DVFSBlackListDialog().show(getFragmentManager(), "DVFSWhiteList");
                    return true;
                });

                TextViewPreference textViewInformationHeader;
                PreferenceScreen ps = (PreferenceScreen) findPreference("prefsRoot");
                textViewInformationHeader = (TextViewPreference) findPreference("fkHeader");
                textViewInformationHeader.setTitle("");

                if (!XposedChecker.isActive()) {
                    textViewInformationHeader.setTitle(R.string.firefds_kit_is_not_active);
                    textViewInformationHeader.getTextView().setTextColor(Color.RED);
                    ps.findPreference("fkHeader").setEnabled(false);
                } else {
                    ps.removePreference(textViewInformationHeader);
                }

                new CheckRootTask().execute();

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        private void showRootDisclaimer() {
            if (mContext != null) {
                try {

                    if (mDialog != null) {
                        mDialog.dismiss();
                    }

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

                    alertDialogBuilder.setTitle(R.string.app_name);

                    alertDialogBuilder.setMessage(R.string.root_info)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                            .setCancelable(true);

                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        private void showDiag() {
            mDialog = new ProgressDialog(getActivity());
            mDialog.setMessage(getString(R.string.checking_root_access));
            mDialog.setCancelable(false);
            mDialog.show();
            showDelayedRootMsg();
        }

        private void showDelayedRootMsg() {

            MainApplication.getHandler().postDelayed(delayedRoot, 20000);

        }

        @Override
        public void onDestroy() {
            try {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.cancel();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            super.onDestroy();
        }

        private class CheckRootTask extends AsyncTask<Void, Void, Void> {
            private boolean suAvailable = false;

            protected Void doInBackground(Void... params) {
                try {
                    suAvailable = Shell.SU.available();

                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(Void p) {

                try {
                    MainApplication.getHandler().removeCallbacks(delayedRoot);
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                    // Check for root access
                    if (!suAvailable) {
                        showRootDisclaimer();
                    } else {
                        Objects.requireNonNull(mDialog)
                                .setMessage(res.getString(R.string.loading_application_preferences_));
                        if (!mDialog.isShowing()) {
                            mDialog.show();
                        }
                        new CopyCSCTask().execute(mContext);

                        if (!MainApplication.getSharedPreferences()
                                .getBoolean("isFirefdsKitFirstLaunch", false)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setCancelable(true)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.firefds_xposed_disclaimer)
                                    .setPositiveButton(R.string.ok_btn, (dialog, which) -> dialog.dismiss())
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .create()
                                    .show();
                            MainApplication.getSharedPreferences()
                                    .edit()
                                    .putBoolean("isFirefdsKitFirstLaunch", true)
                                    .apply();
                            fixPermissions(mContext);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        private static class CopyCSCTask extends AsyncTask<Context, Void, Void> {

            protected Void doInBackground(Context... params) {
                try {
                    XCscFeaturesManager.getDefaultCSCFeatures();
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    XCscFeaturesManager.getDefaultCSCFeaturesFromFiles();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                try {
                    Utils.createCSCFiles(mContext);
                    if (mDialog != null && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }
                super.onPostExecute(result);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            registerPrefsReceiver();
        }

        @Override
        public void onPause() {
            super.onPause();
            unregisterPrefsReceiver();
        }

        private void registerPrefsReceiver() {
            MainApplication.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        private void unregisterPrefsReceiver() {
            MainApplication.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            try {
                // No reboot notification required
                String[] litePrefs = new String[]{"isFirefdsKitFirstLaunch", "screenTimeoutSeconds",
                        "screenTimeoutMinutes", "screenTimeoutHours", "hideCarrierLabel",
                        "carrierSize", "enableCallAdd", "enableCallRecordingMenu",
                        "enableAutoCallRecording", "enable4WayReboot", "mRebootConfirmRequired",
                        "disablePowerMenuLockscreen"};

                setTimeoutPrefs(sharedPreferences, key);

                for (String string : litePrefs) {
                    if (key.equalsIgnoreCase(string)) {
                        return;
                    }
                }

                // Add preference key to changed keys list
                if (!changesMade.contains(key)) {
                    changesMade.add(key);
                }

                RebootNotification.notify(getActivity(), changesMade.size(), true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        private void setTimeoutPrefs(SharedPreferences sharedPreferences, String key) {

            String[] timeoutPrefs = new String[]{"screenTimeoutSeconds", "screenTimeoutMinutes",
                    "screenTimeoutHours"};
            int timeoutML = 0;

            if (key.equalsIgnoreCase(timeoutPrefs[0])) {
                timeoutML += sharedPreferences.getInt(key, 30) * 1000;
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, timeoutML);
            }
            if (key.equalsIgnoreCase(timeoutPrefs[1])) {
                timeoutML += sharedPreferences.getInt(key, 0) * 60000;
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, timeoutML);
            }
            if (key.equalsIgnoreCase(timeoutPrefs[2])) {
                timeoutML += sharedPreferences.getInt(key, 0) * 3600000;
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, timeoutML);
            }
        }
    }
}