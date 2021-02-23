package com.example.walkingtours;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.walkingtours.domain.Building;
import com.example.walkingtours.domain.FenceData;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceBroadcastReceiv";
    private static final String NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing Event Error: " + geofencingEvent.getErrorCode());
            return;
        }

        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        for (Geofence triggeringGeofence : triggeringGeofences) {
            Building building = FenceManager.getBuilding(triggeringGeofence.getRequestId());
            sendNotification(context, building);
        }
    }

    private void sendNotification(Context context, Building building) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(createNotificationChannel(context));
        }

        Intent intent = new Intent(context.getApplicationContext(), BuildingActivity.class);
        intent.putExtra(context.getString(R.string.building_name), building.getId());
        intent.putExtra(context.getString(R.string.building_address), building.getAddress());
        intent.putExtra(context.getString(R.string.building_image_url), building.getImageUrl());
        intent.putExtra(context.getString(R.string.building_description), building.getDescription());

        PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(),
                building.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.fence_notif)
                .setContentTitle(building.getId() + " (Tap to See Details)")
                .setSubText(building.getId()) // small text at top left
                .setContentText(building.getAddress()) // Detail info
                .setVibrate(new long[] {1, 1, 1})
                .setAutoCancel(true)
                .setLights(0xff0000ff, 300, 1000) // blue color
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build();

        notificationManager.notify(getUniqueId(), notification);
    }

    private static int getUniqueId() {
        return(int) (System.currentTimeMillis() % 10000);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel createNotificationChannel(Context context) {
        String name = context.getString(R.string.app_name);
        Uri notificationSound = Uri.parse(getNotificationSoundUri(context));
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();

        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                name, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setSound(notificationSound, audioAttributes);

        return notificationChannel;
    }

    private String getNotificationSoundUri(Context context) {
        return ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + context.getPackageName() + "/" + R.raw.notif_sound;
    }
}
