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
package org.jraf.android.ticker.provider.manager

import android.content.Context
import org.jraf.android.ticker.message.MessageQueueable
import org.jraf.android.ticker.provider.btc.BtcProvider
import org.jraf.android.ticker.provider.datetimeweather.DateTimeWeatherProvider
import org.jraf.android.ticker.provider.twitter.TwitterProvider
import org.jraf.android.ticker.provider.udp.UdpProvider
import java.util.ArrayList

object ProviderManager {
    private val PROVIDER_CLASSES = arrayOf(
            UdpProvider::class.java,
            TwitterProvider::class.java,
            DateTimeWeatherProvider::class.java,
            BtcProvider::class.java
    )

    private val mProviderEntries = ArrayList<ProviderEntry>(4)
    private var mStarted: Boolean = false

    fun startProviders(context: Context, messageQueueable: MessageQueueable) {
        if (mStarted) return
        mStarted = true

        for (providerClass in PROVIDER_CLASSES) {
            val providerEntry = ProviderEntry(context, providerClass, messageQueueable)
            mProviderEntries.add(providerEntry)
            providerEntry.start()
        }
    }

    fun stopProviders() {
        if (!mStarted) return
        mStarted = false
        for (providerEntry in mProviderEntries) {
            providerEntry.stop()
        }
    }
}