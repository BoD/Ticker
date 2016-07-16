package org.jraf.android.ticker.provider.datetimeweather.weather.forecastio;

public class WeatherResult {
    public long timestamp;
    public float currentTemperature;
    public float todayMinTemperature;
    public float todayMaxTemperature;
    public WeatherCondition todayWeatherCondition;

    public WeatherResult() {
        timestamp = System.currentTimeMillis();
    }
}
