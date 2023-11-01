package news.androidtv.launchonboot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.widget.Toast;

/**
 * Created by Nick on 5/11/2017.
 *
 * A foreground service that listens for Screensaver events and responds.
 */
public class DreamListenerService extends Service {
    private static final String TAG = DreamListenerService.class.getSimpleName();

    private static final int ONGOING_NOTIFICATION_ID = 1;

    private BroadcastReceiver dreamHandler = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Redirect intent.
            Log.d(TAG, "Received service event: " + intent.getAction());
            BootReceiver.processEvent(context, intent);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create a foreground service.
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.banner))
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setPriority(Notification.PRIORITY_MIN);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    getString(R.string.default_notification_channel_id),
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(ONGOING_NOTIFICATION_ID, notificationBuilder.build());

//        val notificationBuilder: NotificationCompat.Builder =
//                NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id)).setContentTitle(title)
//                        .setContentText(body).setAutoCancel(true)
//                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) //.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.win))
//                        .setContentIntent(pendingIntent)
//                        .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
//                        .setDefaults(Notification.DEFAULT_VIBRATE)
//                        .setSmallIcon(R.drawable.ic_notification)
//        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        val channel = NotificationChannel(
//                getString(R.string.app_name),
//                getString(R.string.app_name),
//                NotificationManager.IMPORTANCE_DEFAULT
//        )
//        notificationManager.createNotificationChannel(channel)
//        notificationManager.notify(0, notificationBuilder.build())
        Log.d(TAG, "Deploy notification");

        // Register listeners.
        IntentFilter filter = new IntentFilter(Intent.ACTION_DREAMING_STOPPED);
//        registerReceiver(dreamHandler, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(dreamHandler,filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister listener.
//        unregisterReceiver(dreamHandler);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dreamHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
