package com.example.walkingtours;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int LOCATION_REQUEST = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (hasAllLocationPermissions()) {
            openMapsActivity();
        }
    }

    private void openMapsActivity() {
        Intent intent = new Intent(SplashActivity.this, MapsActivity.class);
        startActivity(intent);
//            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        finish();
    }

    private boolean hasAllLocationPermissions() {
        List<String> permissions = new ArrayList<>();
        if (!hasFineLocationAccess()) {
            permissions.add(ACCESS_FINE_LOCATION);
        }
        if (!hasBackgroundLocationAccess()) {
            permissions.add(ACCESS_BACKGROUND_LOCATION);
        }

        if (permissions.isEmpty()) {
            return true;
        } else {
            ActivityCompat.requestPermissions(
                    this, permissions.toArray(new String[0]), LOCATION_REQUEST);
            return false;
        }
    }

    private boolean hasFineLocationAccess() {
        return PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);

    }

    private boolean hasBackgroundLocationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION);
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (LOCATION_REQUEST == requestCode) {
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