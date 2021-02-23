package com.example.walkingtours;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.walkingtours.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
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
    private final static float ZOOM_DEFAULT = 17.0f;

    public int screenHeight;
    public int screenWidth;

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private List<LatLng> travelPathLatLngs = new ArrayList<>();
    private List<LatLng> tourPathLatLngs = new ArrayList<>();
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Polyline travelPathPolyline;
    private Polyline tourPathPolyline;
    private Geocoder geocoder;
    private Marker walkerMarker;
    private FenceManager fenceManager;

    private TextView currentLocationTextView;
    private CheckBox showAddressesCheckbox;
    private CheckBox showGeofencesCheckbox;
    private CheckBox showTravelPathCheckbox;
    private CheckBox showTourPathCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getScreenDimensions();
        geocoder = new Geocoder(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        currentLocationTextView = findViewById(R.id.currentLocationAddressView);
        showAddressesCheckbox = findViewById(R.id.showAddressesCheckbox);
        showGeofencesCheckbox = findViewById(R.id.showGeofencesCheckbox);
        showTravelPathCheckbox = findViewById(R.id.showTravelPathCheckbox);
        showTourPathCheckbox = findViewById(R.id.showTourPathCheckbox);

        fenceManager = new FenceManager(this);
    }

    private void getScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
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

        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        determineLocation();
        setupZoomListener();
    }

    private void setupZoomListener() {
        // todo if time
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
        travelPathLatLngs.add(latLng);

        if (travelPathPolyline != null) {
            travelPathPolyline.remove();
        }

        if (travelPathLatLngs.size() == 1) {
            createInitialLocation(latLng);
        } else {
            addAdditionalLocation(latLng);
        }

        populateAddressField(latLng);
        updateWalkerMarker(latLng);
    }

    private void updateWalkerMarker(LatLng latLng) {
        double radius = getRadius();
        if (Double.isNaN(radius)) {
            return;
        }
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.walker_right);
        Bitmap resizedIcon = Bitmap.createScaledBitmap(icon, (int) radius, (int) radius, false);
        BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resizedIcon);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(iconBitmap);
        markerOptions.position(latLng);

        if (walkerMarker != null) {
            walkerMarker.remove();
        }

        walkerMarker = mMap.addMarker(markerOptions);
    }

    private double getRadius() {
        double zoom = mMap.getCameraPosition().zoom;
        double factor = ((35.0 / 2.0 * zoom) - (355.0 / 2.0));
        double multiplier = ((7.0f / 7200.0f) * screenWidth) - (1.0f / 20.0f);
        return factor * multiplier;
    }

    private void createInitialLocation(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().alpha(0.5f).position(latLng).title("My Starting Point"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_DEFAULT));
    }

    private void populateAddressField(LatLng latLng) {
        if (showAddressesCheckbox.isChecked()) {
            List<Address> currentLocationAddresses = new ArrayList<>();
            try {
                currentLocationAddresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            } catch (IOException e) {
                Log.w(TAG, "Could not get address of current location");
            }
            if (!currentLocationAddresses.isEmpty()) {
                currentLocationTextView.setText(currentLocationAddresses.get(0).getAddressLine(0));
            }
        } else {
            currentLocationTextView.setText("");
        }
    }

    private void addAdditionalLocation(LatLng latLng) {
        if (showTravelPathCheckbox.isChecked()) {
            PolylineOptions polylineOptions = new PolylineOptions().color(getColor(R.color.maps_background));
            polylineOptions.addAll(travelPathLatLngs);
            travelPathPolyline = mMap.addPolyline(polylineOptions);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_DEFAULT));
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

    public GoogleMap getMap() {
        return mMap;
    }

    public void setTourPath(List<LatLng> newLatLngs) {
        tourPathLatLngs.clear();
        tourPathLatLngs.addAll(newLatLngs);

        if (showTourPathCheckbox.isChecked()) {
            drawTourPathPolyline();
        }
    }

    private void drawTourPathPolyline() {
        if (tourPathPolyline != null) {
            tourPathPolyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions().color(getColor(R.color.tour_path));
        polylineOptions.addAll(tourPathLatLngs);
        tourPathPolyline = mMap.addPolyline(polylineOptions);
    }

    public void toggleTourPathPolyLine(View view) {
        if (tourPathPolyline == null) {
            return;
        }
        if (showTourPathCheckbox.isChecked()) {
            tourPathPolyline.setVisible(true);
        } else {
            tourPathPolyline.setVisible(false);
        }
    }

    public void toggleTraveledPathPolyline(View view) {
        if (travelPathPolyline == null) {
            return;
        }
        if (showTravelPathCheckbox.isChecked()) {
            travelPathPolyline.setVisible(true);
        } else {
            travelPathPolyline.setVisible(false);
        }
    }

    public void toggleAddress(View view) {
        if (showAddressesCheckbox.isChecked()) {
            if (travelPathLatLngs.isEmpty()) {
                return;
            }
            populateAddressField(travelPathLatLngs.get(travelPathLatLngs.size() - 1));
        } else {
            currentLocationTextView.setText("");
        }
    }
}