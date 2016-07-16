package org.jraf.android.ticker.provider.datetimeweather;

import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateUtils;

import org.jraf.android.ticker.R;
import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.ProviderException;
import org.jraf.android.ticker.provider.datetimeweather.weather.WeatherManager;
import org.jraf.android.ticker.provider.datetimeweather.weather.WeatherResult;
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
            mCallbacks.addUrgent(date, time);

            // Weather
            WeatherManager weatherManager = WeatherManager.get(mContext);
            WeatherResult weatherResult = weatherManager.getWeather(WeatherManager.TemperatureUnit.CELCIUS);
            if (weatherResult != null) {
                String weatherStr = mContext.getString(R.string.main_weather, weatherResult.temperature, weatherResult.conditionsSymbols);
                mCallbacks.addUrgent(weatherStr);
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