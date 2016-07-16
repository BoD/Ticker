/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.android.ticker.provider.datetimeweather.weather.forecastio;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;

import org.jraf.android.util.log.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class LocationUtil {
    private static final long LAST_LOCATION_MAX_AGE_MS = TimeUnit.HOURS.toMillis(1);

    /**
     * Retrieves a recent location.
     */
    @WorkerThread
    @Nullable
    public static Location getRecentLocation(Context context, long timeout, TimeUnit unit) {
        Log.d("timeout=%s unit=%s", timeout, unit);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("ACCESS_FINE_LOCATION permission not granted: returning null");
            return null;
        }

        final AtomicBoolean connectionFailed = new AtomicBoolean(false);
        GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle params) {
                Log.d("params=%s", params);
            }

            @Override
            public void onConnectionSuspended(int reason) {
                Log.d("reason=%s", reason);
                connectionFailed.set(true);
            }
        };

        GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.d("connectionResult=%s", connectionResult);
            }
        };

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .build();
        googleApiClient.blockingConnect();

        if (connectionFailed.get()) return null;
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation != null && System.currentTimeMillis() - lastLocation.getTime() < LAST_LOCATION_MAX_AGE_MS) {
            Log.d("Got last location: %s", lastLocation);
            if (googleApiClient.isConnected()) googleApiClient.disconnect();
            return lastLocation;
        }

        // Didn't get last location: try to get the current one
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(TimeUnit.SECONDS.toMillis(1));
        locationRequest.setNumUpdates(1);
        locationRequest.setExpirationDuration(unit.toMillis(timeout));
        final AtomicReference<Location> location = new AtomicReference<>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location l) {
                Log.d("Got a location changed event: %s", l);
                location.set(l);
                countDownLatch.countDown();
            }
        };

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener, Looper.getMainLooper());
        try {
            countDownLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            // Should never happen
        }

        // Remove updates and disconnect
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener);
        if (googleApiClient.isConnected()) googleApiClient.disconnect();

        if (location.get() == null) Log.d("Did not get any location update, and timeout expired: returning null");
        return location.get();
    }
}
