package news.androidtv.launchonboot;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.felkertech.settingsmanager.SettingsManager;

import java.util.List;

import static android.view.View.GONE;
import static news.androidtv.launchonboot.SettingsManagerConstants.ONBOARDING;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    private SettingsManager mSettingsManager;
    private SwitchCompat mSwitchEnabled;
    private SwitchCompat mSwitchLiveChannels;
    private SwitchCompat mSwitchWakeup;
    private Button mButtonSelectApp;
    private TextView mPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSettingsManager = new SettingsManager(this);
        if (!mSettingsManager.getBoolean(ONBOARDING)) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchLiveChannels = ((SwitchCompat) findViewById(R.id.switch_live_channels));
        mSwitchEnabled = ((SwitchCompat) findViewById(R.id.switch_enable));
        mSwitchWakeup = ((SwitchCompat) findViewById(R.id.switch_wakeup));
        mButtonSelectApp = (Button) findViewById(R.id.button_select_app);
        mPackageName = ((TextView) findViewById(R.id.text_package_name));

        mSwitchEnabled.setChecked(
                mSettingsManager.getBoolean(SettingsManagerConstants.BOOT_APP_ENABLED));
        mSwitchLiveChannels.setChecked(
                mSettingsManager.getBoolean(SettingsManagerConstants.LAUNCH_LIVE_CHANNELS));
        mSwitchWakeup.setChecked(
                mSettingsManager.getBoolean(SettingsManagerConstants.ON_WAKEUP));
        mPackageName
                .setText(mSettingsManager.getString(SettingsManagerConstants.LAUNCH_ACTIVITY));
        updateSelectionView();

        mSwitchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettingsManager.setBoolean(
                        SettingsManagerConstants.BOOT_APP_ENABLED, isChecked);
                updateSelectionView();
            }
        });
        mSwitchLiveChannels.setOnCheckedChangeListener
                (new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mSettingsManager.setBoolean(
                                SettingsManagerConstants.LAUNCH_LIVE_CHANNELS, isChecked);
                        updateSelectionView();
                    }
                });
        mSwitchWakeup.setOnCheckedChangeListener
                (new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mSettingsManager.setBoolean(
                                SettingsManagerConstants.ON_WAKEUP, isChecked);
                        updateSelectionView();
                        if (isChecked) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                if (!Settings.canDrawOverlays(getApplicationContext())) {
                                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                                }else{
                                    startForegroundService();
                                }
                            }
                            startForegroundService();
                        }
                    }
                });

        if (!getResources().getBoolean(R.bool.DEBUG_FLAG_TEST_BUTTON)) {
            findViewById(R.id.button_test).setVisibility(GONE);
        }
        findViewById(R.id.button_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, BootReceiver.class);
                sendBroadcast(i);
            }
        });

//        findViewById(R.id.button_webview).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent i = new Intent(MainActivity.this, WebViewActivity.class);
//                startActivity(i);
//            }
//        });

        mButtonSelectApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, android.R.style.Theme_Material_Light_Dialog))
                        .setTitle("Select an app")
                        .setItems(getAppNames(getLauncherApps()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String packageName = getPackageName(getLauncherApps().get(which));
                                mSettingsManager.setString(SettingsManagerConstants.LAUNCH_ACTIVITY,
                                        packageName);
                                mPackageName.setText(packageName);
                            }
                        })
                        .show();
            }
        });
        mButtonSelectApp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundColor(hasFocus ? getResources().getColor(R.color.colorAccent) :
                        getResources().getColor(R.color.colorPrimaryDark));
            }
        });
        findViewById(R.id.button_test).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundColor(hasFocus ? getResources().getColor(R.color.colorAccent) :
                        getResources().getColor(R.color.colorPrimaryDark));
            }
        });

        if (DEBUG) {
            Log.d(TAG, getLauncherApps().toString());
            getAppNames(getLauncherApps());
        }
        registerReceiver(new BootReceiver(), new IntentFilter(Intent.ACTION_SCREEN_ON));

        if (mSettingsManager.getBoolean(SettingsManagerConstants.ON_WAKEUP)) {
            startForegroundService();
        }
    }

    public List<ResolveInfo> getLauncherApps() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        // Change which category is used based on form factor.
        if (getResources().getBoolean(R.bool.IS_TV)) {
            mainIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        } else {
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        return getPackageManager().queryIntentActivities(mainIntent, 0);
    }

    public String[] getAppNames(List<ResolveInfo> leanbackApps) {
        String[] appNames = new String[leanbackApps.size()];
        for (int i = 0; i < leanbackApps.size(); i++) {
            ResolveInfo info = leanbackApps.get(i);
            appNames[i] = info.loadLabel(this.getPackageManager()).toString();
            Log.d(TAG, info.loadLabel(this.getPackageManager()).toString());
            Log.d(TAG, info.activityInfo.toString());
            Log.d(TAG, info.activityInfo.name);
        }
        return appNames;
    }

    public String getPackageName(ResolveInfo resolveInfo) {
        return resolveInfo.activityInfo.packageName;
    }

    private void updateSelectionView() {
        if (mSwitchEnabled.isChecked()) {
            mSwitchLiveChannels.setEnabled(true);
            findViewById(R.id.button_test).setEnabled(true);
            if (mSwitchLiveChannels.isChecked()) {
                mButtonSelectApp.setVisibility(GONE);
                mPackageName.setVisibility(GONE);
            } else {
                mButtonSelectApp.setVisibility(View.VISIBLE);
                mPackageName.setVisibility(View.VISIBLE);
            }
        } else {
            mButtonSelectApp.setVisibility(GONE);
            mPackageName.setVisibility(GONE);
            mSwitchLiveChannels.setEnabled(false);
            findViewById(R.id.button_test).setEnabled(false);
        }
    }

    private void startForegroundService() {
        // Ideally only starts once :thinking-emoji:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent i = new Intent(MainActivity.this, DreamListenerService.class);
            startForegroundService(i);
        } else {
            Intent i = new Intent(MainActivity.this, DreamListenerService.class);
            startService(i);
        }
    }
}
