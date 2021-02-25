package com.example.walkingtours;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int COMBO_LOC_REQ = 111;
    private static final int FINE_LOC_ONLY_REQ = 222;
    private static final int BACKGROUND_LOC_ONLY_REQ = 333;
    private static final int ACCURACY_REQUEST = 444;

    private static final int SPLASH_TIME_OUT = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        checkLocationAccuracy();
    }

    private void checkLocationAccuracy() {

        Log.d(TAG, "checkLocationAccuracy: ");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            Log.d(TAG, "onSuccess: High Accuracy Already Present");
            doPermissionCheck();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, ACCURACY_REQUEST);
                } catch (IntentSender.SendIntentException sendEx) {
                    sendEx.printStackTrace();
                }
            }
        });

    }

    private void doPermissionCheck() {
        if (checkPermissions()) {
            new Handler().postDelayed(this::openMapsActivity, SPLASH_TIME_OUT);
        }
    }

    private void openMapsActivity() {
        Intent intent = new Intent(SplashActivity.this, MapsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACCURACY_REQUEST && resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult: ACCURACY_REQUEST granted");
            doPermissionCheck();
        } else {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("High-Accuracy Location Services Required");
            builder.setMessage("High-Accuracy Location Services Required");
            builder.setPositiveButton("OK", (dialog, id) -> finish());
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private boolean checkPermissions() {
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
                openMapsActivity();
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
                openMapsActivity();
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