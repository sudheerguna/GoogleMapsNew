package com.product.samplemaps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    GoogleApiClient googleApiClient;
    GoogleMap mMap;
    LocationRequest locationRequest;
    EditText et_address;
    int mappos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        et_address = findViewById(R.id.et_address);
        mappos = 0;
        initilizeMap();

        et_address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mappos = 1;
                getplaces();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mappos = 0;
    }

    private void initilizeMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setMyLocationEnabled(true);
        googleMap.setTrafficEnabled(true);
        googleMap.setIndoorEnabled(true);
        googleMap.setBuildingsEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if(mappos == 0){
            if (googleMap != null) {
                mMap = googleMap;
                getlatlong();
                calllocationmanager();

                googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        Log.e("cameraPosition", "#" + cameraPosition.target.latitude);
                        getaddressfromlatlong(cameraPosition.target.latitude, cameraPosition.target.longitude);
                    }
                });
            }
        }
    }

    private void getlatlong() {
        googleApiClient = getInstance();
        if (googleApiClient != null) {
            //googleApiClient.connect();
            settingsrequest();
            googleApiClient.connect();
        }
    }

    private void calllocationmanager() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location myLocation = manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 100, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    // TODO Auto-generated method stub
                    Log.e("onLocationChanged", "#" + location.getLatitude());
                }

                @Override
                public void onProviderDisabled(String provider) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onProviderEnabled(String provider) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onStatusChanged(String provider, int status,
                                            Bundle extras) {
                    // TODO Auto-generated method stub
                }
            });

            Log.e("myLocation", "#" + myLocation.getLatitude());
        } catch (Exception e) {

        }
    }

    public GoogleApiClient getInstance() {
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this).addConnectionCallbacks(MainActivity.this).addOnConnectionFailedListener(MainActivity.this)
                .addApi(LocationServices.API).build();
        return mGoogleApiClient;
    }

    public void settingsrequest() {
        Log.e("settingsrequest", "Comes");

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        Log.e("Application", "Button Clicked" + result.getLocationSettingsStates().isLocationPresent());
                        getlocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        // Log.e("Application","Button Clicked1");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                            Log.e("Applicationsett", e.toString());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        //Log.e("Application","Button Clicked2");
                        Toast.makeText(MainActivity.this, "Location is Enabled", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void getlocation() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location == null) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, MainActivity.this);

        } else {
            //If everything went fine lets get latitude and longitude
            double currentLatitude = location.getLatitude();
            double currentLongitude = location.getLongitude();

            MarkerOptions marker = new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Hello Maps ");
//            mMap.addMarker(marker);

            movecamerapos(currentLatitude,currentLongitude);
            Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG).show();
        }
    }

    private void movecamerapos(double currentLatitude, double currentLongitude){
        CameraPosition cameraPosition = new CameraPosition.Builder().target(
                new LatLng(currentLatitude, currentLongitude)).zoom(16).build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void getaddressfromlatlong(double latitude, double longitude) {
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses  = geocoder.getFromLocation(latitude,longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                Log.e("address", "#" + address);
                et_address.setText(address);
            }else{
                et_address.setText("");
                et_address.setHint("No address found");
            }

        } catch (Exception e) {
            et_address.setText("");
            et_address.setHint("No address found");
        }
    }

    private void getplaces() {
        int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
        try {
            AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
//                    .setCountry("SG")
                    .build();
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).setFilter(typeFilter)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == 1) {
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
//                    Log.e("Google", "Place: " + place.getName());
                    et_address.setText(place.getName() + " , " + place.getAddress());

                    String address = place.getName().toString() + "," + place.getAddress().toString();
                    String lat = String.valueOf(place.getLatLng().latitude);
                    String lng = String.valueOf(place.getLatLng().longitude);

                    movecamerapos(place.getLatLng().latitude,place.getLatLng().longitude);
//                    Log.e("lng", "#" + lng);
                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(this, data);
                    // TODO: Handle the error.
//                    Log.i("Google", status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(Location location) {
        Log.e("getLatitude", "#" + location.getLatitude());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


}
