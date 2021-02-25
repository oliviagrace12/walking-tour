package com.example.walkingtours;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class BuildingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.drawable.home_image);
        }

        TextView nameTextView = findViewById(R.id.buildingName);
        TextView addressTextView = findViewById(R.id.buildingAddress);
        ImageView imageTextView = findViewById(R.id.buildingImage);
        TextView descriptionTextView = findViewById(R.id.buildingDescription);

        nameTextView.setText(getIntent().getStringExtra(getString(R.string.building_name)));
        addressTextView.setText(getIntent().getStringExtra(getString(R.string.building_address)));
        descriptionTextView.setText(getIntent().getStringExtra(getString(R.string.building_description)));

        String buildingImageUrl = getIntent().getStringExtra(getString(R.string.building_image_url));

    }
}