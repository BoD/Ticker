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
package org.jraf.android.ticker.provider.datetimeweather.weather

enum class WeatherCondition constructor(private val mCode: String, val symbol: String) {
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
    PARTLY_CLOUDY_NIGHT("partly-cloudy-night", "☁\uD83C\uDF19");

    companion object {
        fun fromCode(code: String): WeatherCondition {
            for (weatherCondition in values()) {
                if (weatherCondition.mCode == code) return weatherCondition
            }

            return UNKNOWN
        }
    }
}
