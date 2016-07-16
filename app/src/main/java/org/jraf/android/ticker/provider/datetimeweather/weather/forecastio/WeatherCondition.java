package org.jraf.android.ticker.provider.datetimeweather.weather.forecastio;

public enum WeatherCondition {
    UNKNOWN("unknown", "?"),
    CLEAR_DAY("clear-day", "☀"),
    CLEAR_NIGHT("clear-night", "\uD83C\uDF19"),
    RAIN("rain", "☂"),
    SNOW("snow", "☃"),
    SLEET("sleet", "☃"),
    WIND("wind", "\uD83D\uDCA8"),
    FOG("fog", "\uD83C\uDF2B"),
    CLOUDY("cloudy", "☁"),
    PARTLY_CLOUDY_DAY("partly-cloudy-day", "\uD83C\uDF24"),
    PARTLY_CLOUDY_NIGHT("partly-cloudy-night", "☁\uD83C\uDF19"),;

    private final String mCode;
    private final String mSymbol;

    WeatherCondition(String code, String symbol) {
        mCode = code;
        mSymbol = symbol;
    }

    public static WeatherCondition fromCode(String code) {
        for (WeatherCondition weatherCondition : values()) {
            if (weatherCondition.mCode.equals(code)) return weatherCondition;
        }

        return UNKNOWN;
    }

    public String getSymbol() {
        return mSymbol;
    }
}
