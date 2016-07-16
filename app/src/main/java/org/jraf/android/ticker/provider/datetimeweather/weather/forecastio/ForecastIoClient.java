package org.jraf.android.ticker.provider.datetimeweather.weather.forecastio;

import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.jraf.android.ticker.R;
import org.jraf.android.util.log.Log;

import com.github.dvdme.ForecastIOLib.FIOCurrently;
import com.github.dvdme.ForecastIOLib.FIODaily;
import com.github.dvdme.ForecastIOLib.FIODataPoint;
import com.github.dvdme.ForecastIOLib.ForecastIO;

public class ForecastIoClient {
    @SuppressLint("StaticFieldLeak")
    private static final ForecastIoClient INSTANCE = new ForecastIoClient();

    private static final long LAST_RESULT_MAX_AGE_MS = TimeUnit.MINUTES.toMillis(10);

    private Context mContext;
    private WeatherResult mLastResult;

    public static ForecastIoClient get(Context context) {
        INSTANCE.mContext = context.getApplicationContext();
        return INSTANCE;
    }

    private ForecastIoClient() {}

    @WorkerThread
    @Nullable
    public WeatherResult getWeather() {
        if (mLastResult != null && System.currentTimeMillis() - mLastResult.timestamp <= LAST_RESULT_MAX_AGE_MS) {
            // Use the cached value
            return mLastResult;
        }

        Location location = LocationUtil.getRecentLocation(mContext, 5, TimeUnit.SECONDS);
        if (location == null) {
            Log.w("Could not retrieve current location");
            if (mLastResult != null) return mLastResult;
            return null;
        }

        WeatherResult res = retrieveWeather(location);
        if (res == null) {
            Log.w("Could not retrieve current weather");
            if (mLastResult != null) return mLastResult;
            return null;
        }

        mLastResult = res;
        return res;
    }

    @WorkerThread
    private WeatherResult retrieveWeather(Location location) {
        WeatherResult res = new WeatherResult();

        ForecastIO forecastIo = new ForecastIO(mContext.getString(R.string.apiKeyForecastIo));
        forecastIo.setUnits(ForecastIO.UNITS_SI);
        forecastIo.setExcludeURL("hourly,minutely");

        // Make the actual API call
        forecastIo.getForecast(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));

        // Get current temperature
        FIOCurrently fioCurrently = new FIOCurrently(forecastIo);
        FIODataPoint currentlyDataPoint = fioCurrently.get();
        res.currentTemperature = currentlyDataPoint.temperature().floatValue();

        // Get today's conditions
        FIODaily fioDaily = new FIODaily(forecastIo);
        FIODataPoint todayDataPoint = fioDaily.getDay(0);
        res.todayMinTemperature = todayDataPoint.temperatureMin().floatValue();
        res.todayMaxTemperature = todayDataPoint.temperatureMax().floatValue();
        res.todayWeatherCondition = WeatherCondition.fromCode(fixIncorrectQuotes(todayDataPoint.icon()));

        return res;
    }

    private static String fixIncorrectQuotes(String s) {
        if (s.startsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }
}
