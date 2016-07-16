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
package org.jraf.android.ticker.provider.manager;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import org.jraf.android.ticker.message.MessageQueueable;
import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.datetimeweather.DateTimeWeatherProvider;
import org.jraf.android.ticker.provider.twitter.TwitterProvider;
import org.jraf.android.ticker.provider.udp.UdpProvider;

public class ProviderManager {
    private static final List<Class<? extends Provider>> PROVIDER_CLASSES = new ArrayList<>();

    static {
        PROVIDER_CLASSES.add(UdpProvider.class);
        PROVIDER_CLASSES.add(TwitterProvider.class);
        PROVIDER_CLASSES.add(DateTimeWeatherProvider.class);
    }

    private static ProviderManager INSTANCE;
    private List<ProviderEntry> mProviderEntries = new ArrayList<>(4);
    private boolean mStarted;

    public static ProviderManager get() {
        if (INSTANCE == null) {
            INSTANCE = new ProviderManager();
        }
        return INSTANCE;
    }

    public void startListeners(Context context, MessageQueueable messageQueueable) {
        if (mStarted) return;
        mStarted = true;

        for (Class<? extends Provider> providerClass : PROVIDER_CLASSES) {
            ProviderEntry providerEntry = new ProviderEntry(context, providerClass, messageQueueable);
            mProviderEntries.add(providerEntry);
            providerEntry.start();
        }
    }

    public void stopListeners() {
        if (!mStarted) return;
        mStarted = false;
        for (ProviderEntry providerEntry : mProviderEntries) {
            providerEntry.stop();
        }
    }
}
