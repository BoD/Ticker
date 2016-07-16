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
