package org.jraf.android.ticker.provider.datetimeweather.weather.playservices;

import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import org.jraf.android.util.log.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class WeatherManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public enum TemperatureUnit {
        CELCIUS,
        FAHRENHEIT,
    }

    @SuppressLint("StaticFieldLeak")
    private static WeatherManager sInstance;
    private final Context mContext;
    private GoogleApiClient mGoogleApiClient;

    public static WeatherManager get(Context context) {
        if (sInstance == null) {
            sInstance = new WeatherManager(context);
        }
        return sInstance;
    }

    public WeatherManager(Context context) {
        mContext = context.getApplicationContext();
        initPlayServices();
    }

    private void initPlayServices() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Awareness.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d();
    }

    @Nullable
    private Weather getWeather() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("ACCESS_FINE_LOCATION permission not granted: returning null");
            return null;
        }
        WeatherResult weatherResult = Awareness.SnapshotApi.getWeather(mGoogleApiClient).await(5, TimeUnit.SECONDS);
        if (!weatherResult.getStatus().isSuccess()) {
            Log.w("Could not get the weather: returning null");
            return null;
        }
        Weather weather = weatherResult.getWeather();
        Log.d("Weather=%s", weather);
        return weather;
    }

    @Nullable
    public org.jraf.android.ticker.provider.datetimeweather.weather.playservices.WeatherResult getWeather(TemperatureUnit unit) {
        Weather weather = getWeather();
        if (weather == null) return null;
        org.jraf.android.ticker.provider.datetimeweather.weather.playservices.WeatherResult res =
                new org.jraf.android.ticker.provider.datetimeweather.weather.playservices.WeatherResult();
        res.temperature = weather.getTemperature(unit == TemperatureUnit.CELCIUS ? Weather.CELSIUS : Weather.FAHRENHEIT);
        res.conditionsSymbols = getConditionsSymbols(weather);
        return res;
    }

    private String getConditionsSymbols(Weather weather) {
        int[] conditions = weather.getConditions();
        StringBuilder res = new StringBuilder();
        for (int condition : conditions) {
            switch (condition) {
                case Weather.CONDITION_CLEAR:
                    res.append('☀');
                    break;

                case Weather.CONDITION_CLOUDY:
                    res.append('☁');
                    break;

                case Weather.CONDITION_FOGGY:
                    res.append("\uD83C\uDF2B");
                    break;

                case Weather.CONDITION_HAZY:
                    res.append("\uD83C\uDF2B");
                    break;

                case Weather.CONDITION_ICY:
                    res.append('\u26F8');
                    break;

                case Weather.CONDITION_RAINY:
                    res.append('☂');
                    break;

                case Weather.CONDITION_SNOWY:
                    res.append('☃');
                    break;

                case Weather.CONDITION_STORMY:
                    res.append('\u26C8');
                    break;

                case Weather.CONDITION_WINDY:
                    res.append("\uD83D\uDCA8");
                    break;
            }
        }
        return res.toString();
    }
}
