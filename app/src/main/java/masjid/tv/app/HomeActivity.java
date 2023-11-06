package masjid.tv.app;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import static android.view.View.GONE;

import masjid.tv.app.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {
    private SettingsManager mSettingsManager;
    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        mSettingsManager = new SettingsManager(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.switchEnable.setChecked(
                mSettingsManager.getBoolean(Constants.APP_ENABLED));
        binding.switchLiveChannels.setChecked(
                mSettingsManager.getBoolean(Constants.LAUNCH_LIVE_CHANNELS));
        binding.switchWakeup.setChecked(
                mSettingsManager.getBoolean(Constants.WHEN_WAKEUP));
        binding.textPackageName
                .setText(mSettingsManager.getString(Constants.LAUNCH_ACTIVITY));
        updateSelectionView();

        binding.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mSettingsManager.setBoolean(
                    Constants.APP_ENABLED, isChecked);
            updateSelectionView();
        });
        binding.switchLiveChannels.setOnCheckedChangeListener
                ((buttonView, isChecked) -> {
                    mSettingsManager.setBoolean(
                            Constants.LAUNCH_LIVE_CHANNELS, isChecked);
                    updateSelectionView();
                });
        binding.switchWakeup.setOnCheckedChangeListener
                ((buttonView, isChecked) -> {
                    mSettingsManager.setBoolean(
                            Constants.WHEN_WAKEUP, isChecked);
                    updateSelectionView();
                    if (isChecked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (!Settings.canDrawOverlays(getApplicationContext())) {
                                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                            }else{
                                startBackgroundService();
                            }
                        }
                        startBackgroundService();
                    }
                });
        binding.buttonSelectApp.setOnClickListener(v -> {
           MaterialAlertDialogBuilder dialog_ = new MaterialAlertDialogBuilder(HomeActivity.this)
                    .setTitle("Select an app")
                   .setBackground(ContextCompat.getDrawable(this,R.drawable.background_dialog))
                    .setItems(getAppNames(getApps()), (dialog, which) -> {
                        String packageName = getPackageName(getApps().get(which));
                        mSettingsManager.setString(Constants.LAUNCH_ACTIVITY,
                                packageName);
                        binding.textPackageName.setText(packageName);
                    });
            dialog_.show();
        });
        binding.buttonSelectApp.setOnFocusChangeListener((v, hasFocus) -> v.setBackgroundColor(hasFocus ? getResources().getColor(R.color.colorAccent) :
                getResources().getColor(R.color.colorPrimaryDark)));
        registerReceiver(new AppReceiver(), new IntentFilter(Intent.ACTION_SCREEN_ON));
        if (mSettingsManager.getBoolean(Constants.WHEN_WAKEUP)) {
            startBackgroundService();
        }
    }

    public List<ResolveInfo> getApps() {
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
        }
        return appNames;
    }

    public String getPackageName(ResolveInfo resolveInfo) {
        return resolveInfo.activityInfo.packageName;
    }

    private void updateSelectionView() {
        if (binding.switchEnable.isChecked()) {
            binding.switchLiveChannels.setEnabled(true);
            if (binding.switchLiveChannels.isChecked()) {
                binding.buttonSelectApp.setVisibility(GONE);
                binding.textPackageName.setVisibility(GONE);
            } else {
                binding.buttonSelectApp.setVisibility(View.VISIBLE);
                binding.textPackageName.setVisibility(View.VISIBLE);
            }
        } else {
            binding.buttonSelectApp.setVisibility(GONE);
            binding.textPackageName.setVisibility(GONE);
            binding.switchLiveChannels.setEnabled(false);
        }
    }

    private void startBackgroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent i = new Intent(HomeActivity.this, EventListenerService.class);
            startForegroundService(i);
        } else {
            Intent i = new Intent(HomeActivity.this, EventListenerService.class);
            startService(i);
        }
    }
}
