package com.example.walkingtours;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class BuildingActivity extends AppCompatActivity {

    private static final String TAG = "BuildingActivity";

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.drawable.home_image);
        }

        Typeface customFont = Typeface.createFromAsset(getAssets(), "fonts/Acme-Regular.ttf");

        TextView nameTextView = findViewById(R.id.buildingName);
        TextView addressTextView = findViewById(R.id.buildingAddress);
        imageView = findViewById(R.id.buildingImage);
        TextView descriptionTextView = findViewById(R.id.buildingDescription);

        nameTextView.setTypeface(customFont);
        nameTextView.setText(getIntent().getStringExtra(getString(R.string.building_name)));
        addressTextView.setTypeface(customFont);
        addressTextView.setText(getIntent().getStringExtra(getString(R.string.building_address)));
        descriptionTextView.setTypeface(customFont);
        descriptionTextView.setText(getIntent().getStringExtra(getString(R.string.building_description)));

        String buildingImageUrl = getIntent().getStringExtra(getString(R.string.building_image_url));

        loadImage(buildingImageUrl);
    }

    private void loadImage(String buildingImageUrl) {
        Picasso.get()
                .load(buildingImageUrl)
                .error(R.drawable.logo)
                .placeholder(R.drawable.logo)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Successfully loaded image: Size: " +
                                ((BitmapDrawable) imageView.getDrawable()).getBitmap().getByteCount());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d(TAG, "Error loading image: " + e.getMessage());
                    }
                });
    }
}