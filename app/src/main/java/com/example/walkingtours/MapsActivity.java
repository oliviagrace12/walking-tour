package com.example.walkingtours;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.walkingtours.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private static final int COMBO_LOC_REQ = 111;
    private static final int FINE_LOC_ONLY_REQ = 222;
    private static final int BACKGROUND_LOC_ONLY_REQ = 333;

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private List<LatLng> latLngs = new ArrayList<>();
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Polyline latLngPolyline;
    private final static float ZOOM_DEFAULT = 17.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        determineLocation();
    }

    private void determineLocation() {
        if (checkPermission()) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new MyLocationListener(this);

            if (locationManager != null) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
            }
        }
    }

    public void updateLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        latLngs.add(latLng);

        if (latLngPolyline != null) {
            latLngPolyline.remove();
        }

        if (latLngs.size() == 1) {
            createInitialLocation(latLng);
            return;
        }

        addAdditionalLocation(latLng);
    }

    private void createInitialLocation(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().alpha(0.5f).position(latLng).title("My Starting Point"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_DEFAULT));
    }

    private void addAdditionalLocation(LatLng latLng) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(latLngs);
        latLngPolyline = mMap.addPolyline(polylineOptions);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!hasFineLocationAccess()) {
                ActivityCompat.requestPermissions(this,
                        new String[]{ACCESS_FINE_LOCATION}, FINE_LOC_ONLY_REQ);
                return false;
            } else if (!hasBackgroundLocationAccess()) {
                ActivityCompat.requestPermissions(this,
                        new String[]{ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOC_ONLY_REQ);
                return false;
            } else {
                return true;
            }
        } else {
            List<String> permissions = new ArrayList<>();
            if (!hasFineLocationAccess()) {
                permissions.add(ACCESS_FINE_LOCATION);
            }
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
                    && !hasBackgroundLocationAccess()) {
                permissions.add(ACCESS_BACKGROUND_LOCATION);
            }

            if (permissions.isEmpty()) {
                return true;
            } else {
                ActivityCompat.requestPermissions(
                        this, permissions.toArray(new String[0]), COMBO_LOC_REQ);
                return false;
            }
        }
    }

    private boolean hasFineLocationAccess() {
        return PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);

    }

    private boolean hasBackgroundLocationAccess() {
        return PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (COMBO_LOC_REQ == requestCode) {
            List<String> deniedPermissions = new ArrayList<>();
            int numGranted = 0;
            for (int i = 0; i < permissions.length; i++) {
                if (PERMISSION_GRANTED == grantResults[i]) {
                    numGranted++;
                } else {
                    deniedPermissions.add(permissions[i]);
                }
            }
            if (numGranted == permissions.length) {
                determineLocation();
            } else {
                String deniedPermsString =
                        deniedPermissions.stream().collect(Collectors.joining(","));
                Log.e(TAG, "Permissions not granted: " + deniedPermsString);
                openPermissionsDeniedDialogue(deniedPermsString);
            }
        } else if (FINE_LOC_ONLY_REQ == requestCode) {
            if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBackgroundPermissionOnly();
            } else {
                openPermissionsDeniedDialogue(permissions[0]);
            }
        } else if (BACKGROUND_LOC_ONLY_REQ == requestCode) {
            if (permissions[0].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                determineLocation();
            } else {
                openPermissionsDeniedDialogue(permissions[0]);
            }
        }
    }

    private void checkBackgroundPermissionOnly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasBackgroundLocationAccess()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    BACKGROUND_LOC_ONLY_REQ);
        }
    }

    private void openPermissionsDeniedDialogue(String deniedPermsString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", (dialogue, id) -> finish());
        builder.setTitle("Necessary Permissions Not Granted");
        builder.setMessage(getString(R.string.app_name)
                + " cannot run without the following permissions: " + deniedPermsString);
        builder.setIcon(R.drawable.walker_right);
        builder.create().show();
    }
}