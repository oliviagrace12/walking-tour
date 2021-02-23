package com.example.walkingtours;

import android.location.Location;
import android.location.LocationListener;

import androidx.annotation.NonNull;

public class MyLocationListener implements LocationListener {

    private final MapsActivity mapsActivity;

    public MyLocationListener(MapsActivity mapsActivity) {
        this.mapsActivity = mapsActivity;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        mapsActivity.updateLocation(location);
    }
}
