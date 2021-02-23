package com.example.walkingtours;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.graphics.ColorUtils;

import com.example.walkingtours.domain.Building;
import com.example.walkingtours.domain.FenceData;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FenceManager {

    private static final String TAG = "FenceManager";
    private final MapsActivity mapsActivity;
    private final GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private final List<PatternItem> fencePattern = Collections.singletonList(new Dot());
    private final ArrayList<Circle> circles = new ArrayList<>();
    private static Map<String, Building> buildingsMap = new HashMap<>();
    private static Map<String, FenceData> fenceDataMap = new HashMap<>();


    public FenceManager(MapsActivity mapsActivity) {
        this.mapsActivity = mapsActivity;
        this.geofencingClient = LocationServices.getGeofencingClient(mapsActivity);

        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(mapsActivity, aVoid -> Log.d(TAG, "onSuccess: removeGeofences"))
                .addOnFailureListener(mapsActivity, e -> {
                    Log.d(TAG, "onFailure: removeGeofences");
                    Toast.makeText(mapsActivity, "Trouble removing existing fences: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        new Thread(new DataDownloader(mapsActivity, this)).start();
    }

    public static FenceData getFenceData(String requestId) {
        return fenceDataMap.get(requestId);
    }

    public static Building getBuilding(String id) {
        return buildingsMap.get(id);
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(mapsActivity, GeofenceBroadcastReceiver.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(mapsActivity, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return geofencePendingIntent;
    }

    public void addFenceData(Map<String, FenceData> newFenceDataMap) {
        fenceDataMap.clear();
        fenceDataMap.putAll(newFenceDataMap);

        for (FenceData fenceData : fenceDataMap.values()) {
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(fenceData.getId())
                    .setCircularRegion(fenceData.getLatitude(), fenceData.getLongitude(), fenceData.getRadius())
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build();

            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .addGeofence(geofence).build();

            geofencePendingIntent = getGeofencePendingIntent();

            if (ActivityCompat.checkSelfPermission(mapsActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "onSuccess: addGeofences"))
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Log.d(TAG, "onFailure: addGeofences");

                        Toast.makeText(mapsActivity, "Trouble adding new fence: " + e.getMessage(), Toast.LENGTH_LONG).show();

                    });
        }
        mapsActivity.runOnUiThread(this::drawFences);
    }

    void eraseFences() {
        for (Circle circle : circles) {
            circle.remove();
        }
        circles.clear();
    }

    void drawFences() {
        for (FenceData fenceData : fenceDataMap.values()) {
            drawFence(fenceData);
        }
    }

    private void drawFence(FenceData fenceData) {
        int lineColor = Color.parseColor(fenceData.getFenceColor());
        int lineFill = ColorUtils.setAlphaComponent(lineColor, 85);

        LatLng latLng = new LatLng(fenceData.getLatitude(), fenceData.getLongitude());
        Circle circle = mapsActivity.getMap().addCircle(
                new CircleOptions()
                        .center(latLng)
                        .radius(fenceData.getRadius())
                        .strokePattern(fencePattern)
                        .strokeColor(lineColor)
                        .fillColor(lineFill));

        circles.add(circle);
    }

    void addBuildings(Map<String, Building> newBuildings) {
        buildingsMap.clear();
        buildingsMap.putAll(newBuildings);
    }

    public void showFences() {
        for (Circle circle : circles) {
            circle.setVisible(true);
        }
    }

    public void hideFences() {
        for (Circle circle : circles) {
            circle.setVisible(false);
        }
    }
}

