package com.example.walkingtours;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class BuildingActivity extends AppCompatActivity {

    private Typeface customFont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.drawable.home_image);
        }

        customFont = Typeface.createFromAsset(getAssets(), "fonts/Acme-Regular.ttf");

        TextView nameTextView = findViewById(R.id.buildingName);
        TextView addressTextView = findViewById(R.id.buildingAddress);
        ImageView imageTextView = findViewById(R.id.buildingImage);
        TextView descriptionTextView = findViewById(R.id.buildingDescription);

        nameTextView.setTypeface(customFont);
        nameTextView.setText(getIntent().getStringExtra(getString(R.string.building_name)));
        addressTextView.setTypeface(customFont);
        addressTextView.setText(getIntent().getStringExtra(getString(R.string.building_address)));
        descriptionTextView.setTypeface(customFont);
        descriptionTextView.setText(getIntent().getStringExtra(getString(R.string.building_description)));

        String buildingImageUrl = getIntent().getStringExtra(getString(R.string.building_image_url));

        // TODO load image with Picasso
    }
}