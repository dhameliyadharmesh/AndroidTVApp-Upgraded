package masjid.tv.app;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.os.Build;
import android.widget.Toast;

import com.felkertech.settingsmanager.SettingsManager;

public class AppReceiver extends BroadcastReceiver {
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (intent.getAction() != null &&
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            startForegroundService();
        }
        Toast.makeText(context,"Device Rebooted ",Toast.LENGTH_LONG).show();
        processEvent(context, intent);
    }

    public static void processEvent(Context context, Intent intent) {
        SettingsManager settingsManager = new SettingsManager(context);
        if (!settingsManager.getBoolean(Constants.BOOT_APP_ENABLED)) {
            return;
        }
        if (intent.getAction() != null &&
                intent.getAction().equals(Intent.ACTION_USER_PRESENT) &&
                !settingsManager.getBoolean(Constants.WHEN_WAKEUP)) {
            return;
        }
        if (intent.getAction() != null &&
                intent.getAction().equals(Intent.ACTION_SCREEN_ON) &&
                !settingsManager.getBoolean(Constants.WHEN_WAKEUP)) {
            return;
        }
        if (intent.getAction() != null &&
                intent.getAction().equals(Intent.ACTION_DREAMING_STOPPED) &&
                !settingsManager.getBoolean(Constants.WHEN_WAKEUP)) {
            return;
        }
        if (settingsManager.getBoolean(Constants.LAUNCH_LIVE_CHANNELS) &&
                context.getResources().getBoolean(R.bool.TIF_SUPPORT) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent i = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if (!settingsManager.getString(Constants.LAUNCH_ACTIVITY).isEmpty()) {
            Intent i;
            if (context.getResources().getBoolean(R.bool.IS_TV)) {
                i = context.getPackageManager().getLeanbackLaunchIntentForPackage(
                        settingsManager.getString(Constants.LAUNCH_ACTIVITY));
            } else {
                i = context.getPackageManager().getLaunchIntentForPackage(
                        settingsManager.getString(Constants.LAUNCH_ACTIVITY));
            }

            if (i == null) {
                Toast.makeText(context, R.string.null_intent, Toast.LENGTH_SHORT).show();
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(i);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, R.string.null_intent, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent i = new Intent(mContext, EventListenerService.class);
            mContext.startForegroundService(i);
        } else {
            Intent i = new Intent(mContext, EventListenerService.class);
            mContext.startService(i);
        }

    }
}