package com.example.walkingtours;

import android.net.Uri;
import android.util.Log;

import com.example.walkingtours.domain.Building;
import com.example.walkingtours.domain.FenceData;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataDownloader implements Runnable {

    private static final String TAG = "DataDownloader";

    private final MapsActivity mapsActivity;
    private final FenceManager fenceManager;

    public DataDownloader(MapsActivity mapsActivity, FenceManager fenceManager) {
        this.mapsActivity = mapsActivity;
        this.fenceManager = fenceManager;
    }

    @Override
    public void run() {
        String jsonResponse = requestData();
        try {
            parseData(jsonResponse);
        } catch (JSONException e) {
            Log.e(TAG, "Could not parse fence and path data: " + e.getLocalizedMessage());
        }
    }

    private void parseData(String jsonResponse) throws JSONException {
        JSONObject responseObject = new JSONObject(jsonResponse);
        JSONArray jsonFences = responseObject.getJSONArray("fences");
        Map<String, FenceData> fenceDataMap = parseFenceData(jsonFences);
        fenceManager.addFenceData(fenceDataMap);
        Map<String, Building> buildingMap = parseBuildingData(jsonFences);
        fenceManager.addBuildings(buildingMap);
        List<LatLng> latLngList = parseLatLngData(responseObject.getJSONArray("path"));
        mapsActivity.runOnUiThread(() -> mapsActivity.setTourPath(latLngList));
    }

    private Map<String, Building> parseBuildingData(JSONArray jsonFences) throws JSONException {
        Map<String, Building> buildingMap = new HashMap<>();
        for (int i = 0; i < jsonFences.length(); i++) {
            JSONObject jsonFenceObject = jsonFences.getJSONObject(i);
            Building building = new Building();
            building.setId(jsonFenceObject.getString("id"));
            building.setAddress(jsonFenceObject.getString("address"));
            building.setImageUrl(jsonFenceObject.getString("description"));
            building.setImageUrl(jsonFenceObject.getString("image"));

            buildingMap.put(building.getId(), building);
        }
        return buildingMap;
    }

    private List<LatLng> parseLatLngData(JSONArray jsonPathArray) throws JSONException {
        List<LatLng> latLngList = new ArrayList<>();
        for (int i = 0; i < jsonPathArray.length(); i++) {
            String longLatString = jsonPathArray.getString(i);
            String[] longLat = longLatString.trim().split(",");
            LatLng latLng = new LatLng(Double.parseDouble(longLat[1]), Double.parseDouble(longLat[0]));

            latLngList.add(latLng);
        }

        return latLngList;
    }

    private Map<String, FenceData> parseFenceData(JSONArray jsonFenceArray) throws JSONException {
        Map<String, FenceData> fenceDataMap = new HashMap<>();
        for (int i = 0; i < jsonFenceArray.length(); i++) {
            JSONObject jsonFenceObject = jsonFenceArray.getJSONObject(i);
            FenceData fenceData = new FenceData();
            fenceData.setId(jsonFenceObject.getString("id"));
            fenceData.setLatitude(jsonFenceObject.getDouble("latitude"));
            fenceData.setLongitude(jsonFenceObject.getDouble("longitude"));
            fenceData.setRadius(jsonFenceObject.getLong("radius"));
            fenceData.setFenceColor(jsonFenceObject.getString("fenceColor"));

            fenceDataMap.put(fenceData.getId(), fenceData);
        }

        return fenceDataMap;
    }

    private String requestData() {
        String urlString = createUrlString();
        Log.i(TAG, "Requesting data using URL: " + urlString);
        HttpURLConnection conn = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");
            conn.addRequestProperty("User-Agent", "");
            conn.setRequestProperty("Accept", "application/json");

            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "HTTP ResponseCode NOT OK: " + conn.getResponseCode());
                return "";
            }
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader((new InputStreamReader(is)));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            Log.d(TAG, "Response: " + stringBuilder.toString());
        } catch (IOException ex) {
            Log.e(TAG, "Error in getting info: " + ex.getLocalizedMessage(), ex);
            return "";
        }

        return stringBuilder.toString();
    }

    private String createUrlString() {
        return new Uri.Builder()
                .scheme("http")
                .authority("christopherhield.com")
                .appendPath("data")
                .appendPath("WalkingTourContent.json")
                .build().toString();
    }
}
