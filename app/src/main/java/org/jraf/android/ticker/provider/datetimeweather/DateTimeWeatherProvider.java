package org.jraf.android.ticker.provider.datetimeweather;

import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateUtils;

import org.jraf.android.ticker.R;
import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.ProviderException;
import org.jraf.android.ticker.provider.datetimeweather.weather.forecastio.ForecastIoClient;
import org.jraf.android.ticker.provider.datetimeweather.weather.forecastio.WeatherResult;
import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks;

public class DateTimeWeatherProvider implements Provider {
    private Context mContext;
    private ProviderManagerCallbacks mCallbacks;
    private Handler mDateTimeWeatherHandler;

    @Override
    public void init(Context context, ProviderManagerCallbacks callbacks) throws ProviderException {
        mContext = context;
        mCallbacks = callbacks;
    }

    @Override
    public void start() throws ProviderException {
        startDateTimeWeather();
        mCallbacks.onStart();
    }

    @Override
    public void stop() {
        stopDateTimeWeather();
        mCallbacks.onStop();
    }

    private Runnable mDateTimeWeatherRunnable = new Runnable() {
        @Override
        public void run() {
            // Date, time
            long now = System.currentTimeMillis();
            String date =
                    DateUtils.formatDateTime(mContext, now, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
            String time =
                    DateUtils.formatDateTime(mContext, now, DateUtils.FORMAT_SHOW_TIME);

            // Weather
            ForecastIoClient forecastIoClient = ForecastIoClient.get(mContext);
            WeatherResult weatherResult = forecastIoClient.getWeather();

            // Add everything urgently at once
            if (weatherResult == null) {
                mCallbacks.addUrgent(date, time);
            } else {
                String weatherNow = mContext.getString(R.string.weather_now,
                        weatherResult.todayWeatherCondition.getSymbol(),
                        weatherResult.currentTemperature);
                String weatherMin = mContext.getString(R.string.weather_min, weatherResult.todayMinTemperature);
                String weatherMax = mContext.getString(R.string.weather_max, weatherResult.todayMaxTemperature);
                mCallbacks.addUrgent(date, time, weatherNow, weatherMin, weatherMax);
            }

            startDateTimeWeather();
        }
    };

    private void startDateTimeWeather() {
        if (mDateTimeWeatherHandler == null) {
            HandlerThread thread = new HandlerThread("DateTimeWeather");
            thread.start();
            mDateTimeWeatherHandler = new Handler(thread.getLooper());
        }
        mDateTimeWeatherHandler.postDelayed(mDateTimeWeatherRunnable, TimeUnit.MINUTES.toMillis(1));
    }

    private void stopDateTimeWeather() {
        mDateTimeWeatherHandler.removeCallbacks(mDateTimeWeatherRunnable);
    }
}