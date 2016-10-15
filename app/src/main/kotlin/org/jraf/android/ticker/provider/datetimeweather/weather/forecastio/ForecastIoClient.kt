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
package org.jraf.android.ticker.provider.datetimeweather.weather.forecastio

import android.content.Context
import android.location.Location
import android.support.annotation.WorkerThread
import com.github.dvdme.ForecastIOLib.FIOCurrently
import com.github.dvdme.ForecastIOLib.FIODaily
import com.github.dvdme.ForecastIOLib.ForecastIO
import org.jraf.android.ticker.R
import org.jraf.android.ticker.provider.datetimeweather.weather.LocationUtil
import org.jraf.android.ticker.provider.datetimeweather.weather.WeatherCondition
import org.jraf.android.ticker.provider.datetimeweather.weather.WeatherResult
import org.jraf.android.util.log.Log
import java.util.concurrent.TimeUnit

class ForecastIoClient private constructor(val mContext: Context) {
    companion object {
        private var sInstance: ForecastIoClient? = null

        private val LAST_RESULT_MAX_AGE_MS = TimeUnit.MINUTES.toMillis(10)

        @Synchronized fun get(context: Context): ForecastIoClient {
            if (sInstance == null) sInstance = ForecastIoClient(context.applicationContext)
            return sInstance!!
        }

        private fun fixIncorrectQuotes(s: String): String {
            if (s.startsWith("\"")) return s.substring(1, s.length - 1)
            return s
        }
    }

    private var mLastResult: WeatherResult? = null

    val weather: WeatherResult?
        @WorkerThread
        get() {
            val lastResult = mLastResult
            if (lastResult != null && System.currentTimeMillis() - lastResult.timestamp <= LAST_RESULT_MAX_AGE_MS) {
                // Use the cached value
                return lastResult
            }

            val location = LocationUtil.getRecentLocation(mContext, 5, TimeUnit.SECONDS)
            if (location == null) {
                Log.w("Could not retrieve current location")
                return lastResult
            }

            val res = retrieveWeather(location)
            mLastResult = res
            return res
        }

    @WorkerThread
    private fun retrieveWeather(location: Location): WeatherResult {
        val forecastIo = ForecastIO(mContext.getString(R.string.apiKeyForecastIo))
        forecastIo.units = ForecastIO.UNITS_SI
        forecastIo.excludeURL = "hourly,minutely"

        // Make the actual API call (blocking)
        forecastIo.getForecast(location.latitude.toString(), location.longitude.toString())

        // Get current temperature
        val fioCurrently = FIOCurrently(forecastIo)
        val currentlyDataPoint = fioCurrently.get()
        val currentTemperature = currentlyDataPoint.temperature()?.toFloat() ?: 0F

        // Get today's conditions
        val fioDaily = FIODaily(forecastIo)
        val todayDataPoint = fioDaily.getDay(0)
        val todayMinTemperature = todayDataPoint.temperatureMin()?.toFloat() ?: 0F
        val todayMaxTemperature = todayDataPoint.temperatureMax()?.toFloat() ?: 0F
        val todayWeatherCondition = WeatherCondition.fromCode(fixIncorrectQuotes(todayDataPoint.icon()))

        return WeatherResult(currentTemperature, todayMinTemperature, todayMaxTemperature, todayWeatherCondition)
    }
}
