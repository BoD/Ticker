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
package org.jraf.android.ticker.provider.datetimeweather

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.text.format.DateUtils
import org.jraf.android.ticker.R
import org.jraf.android.ticker.provider.Provider
import org.jraf.android.ticker.provider.ProviderException
import org.jraf.android.ticker.provider.datetimeweather.weather.forecastio.ForecastIoClient
import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks
import java.util.concurrent.TimeUnit

class DateTimeWeatherProvider : Provider {
    private lateinit var mContext: Context
    private lateinit var mCallbacks: ProviderManagerCallbacks
    private val mDateTimeWeatherHandler: Handler by lazy {
        val thread = HandlerThread("DateTimeWeather")
        thread.start()
        Handler(thread.looper)
    }

    @Throws(ProviderException::class)
    override fun init(context: Context, callbacks: ProviderManagerCallbacks) {
        mContext = context
        mCallbacks = callbacks
    }

    @Throws(ProviderException::class)
    override fun start() {
        startDateTimeWeather()
        mCallbacks.onStart()
    }

    override fun stop() {
        stopDateTimeWeather()
        mCallbacks.onStop()
    }

    private val mDateTimeWeatherRunnable = Runnable {
        // Date, time
        val now = System.currentTimeMillis()
        val date = DateUtils.formatDateTime(mContext, now, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR)
        val time = DateUtils.formatDateTime(mContext, now, DateUtils.FORMAT_SHOW_TIME)

        // Weather
        val forecastIoClient = ForecastIoClient.get(mContext)
        val weatherResult = forecastIoClient.weather

        // Add everything urgently at once
        if (weatherResult == null) {
            mCallbacks.addUrgent(date, time)
        } else {
            val weatherNow = mContext.getString(R.string.weather_now,
                    weatherResult.todayWeatherCondition.symbol,
                    weatherResult.currentTemperature)
            val weatherMin = mContext.getString(R.string.weather_min, weatherResult.todayMinTemperature)
            val weatherMax = mContext.getString(R.string.weather_max, weatherResult.todayMaxTemperature)
            mCallbacks.addUrgent(date, time, weatherNow, weatherMin, weatherMax)
        }

        startDateTimeWeather()
    }

    private fun startDateTimeWeather() {
        mDateTimeWeatherHandler.postDelayed(mDateTimeWeatherRunnable, TimeUnit.MINUTES.toMillis(1) + TimeUnit.SECONDS.toMillis(30))

    }

    private fun stopDateTimeWeather() {
        mDateTimeWeatherHandler.removeCallbacks(mDateTimeWeatherRunnable)
    }
}