package com.hepicar.listeneverything;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class GPSService {
    private static final String TAG = "GPSService";
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private double latitude;
    private double longitude;

    private Location mLastKnownLocation;

    private Context context;

    public GPSService(Context context) {
        this.context = context;
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void detectDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                mLastKnownLocation = task.getResult();
                latitude = mLastKnownLocation.getLatitude();
                longitude = mLastKnownLocation.getLongitude();
                Location lc = new Location("current");
                lc.setLatitude(latitude);
                lc.setLongitude(longitude);
            }
        });
    }

    public double getLatitude(){
        return this.latitude;
    }
    public double getLongitude(){
        return this.longitude;
    }
}