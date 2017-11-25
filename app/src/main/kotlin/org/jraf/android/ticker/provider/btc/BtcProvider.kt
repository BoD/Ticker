/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2017 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.ticker.provider.btc

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.double
import com.beust.klaxon.obj
import org.jraf.android.ticker.R
import org.jraf.android.ticker.provider.Provider
import org.jraf.android.ticker.provider.ProviderException
import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class BtcProvider : Provider {
    companion object {
        private const val URL_API = "https://blockchain.info/ticker"
    }

    private lateinit var mContext: Context
    private lateinit var mCallbacks: ProviderManagerCallbacks
    private val mBtcHandler: Handler by lazy {
        val thread = HandlerThread("Btc")
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
        startBtc()
        mCallbacks.onStart()
    }

    override fun stop() {
        stopDateTimeWeather()
        mCallbacks.onStop()
    }

    private val mBtcRunnable = {
        val connection = URL(URL_API).openConnection() as HttpURLConnection
        val value = try {
            val jsonStr = connection.inputStream.bufferedReader().readText()
            val rootJson: JsonObject = Parser().parse(StringBuilder(jsonStr)) as JsonObject
            rootJson.obj("EUR")?.double("15m")
        } finally {
            connection.disconnect()
        }

        // Add urgently at once
        if (value != null) {
            mCallbacks.addUrgent(mContext.getString(R.string.btc_value, value.toInt()))
        }

        startBtc()
    }

    private fun startBtc() {
        mBtcHandler.postDelayed(mBtcRunnable, TimeUnit.MINUTES.toMillis(6))
    }

    private fun stopDateTimeWeather() {
        mBtcHandler.removeCallbacks(mBtcRunnable)
    }
}