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

import android.content.Context;

import org.jraf.android.ticker.message.MessageQueueable;
import org.jraf.android.ticker.provider.Provider;
import org.jraf.android.ticker.provider.ProviderException;
import org.jraf.android.util.log.Log;

class ProviderEntry implements ProviderManagerCallbacks {
    private final Context mContext;
    private final Class<? extends Provider> mProviderClass;
    private final MessageQueueable mMessageQueueable;
    private Provider mProvider;

    ProviderEntry(Context context, Class<? extends Provider> providerClass, MessageQueueable messageQueueable) {
        mContext = context.getApplicationContext();
        mProviderClass = providerClass;
        mMessageQueueable = messageQueueable;
    }

    void start() {
        try {
            mProvider = mProviderClass.newInstance();
        } catch (Exception ignored) {}
        assert mProvider != null;
        try {
            Log.i("Initializing provider " + mProviderClass);
            mProvider.init(mContext, this);
        } catch (ProviderException e) {
            Log.w(e, "Could not init %s: give up", mProviderClass);
            return;
        }

        try {
            Log.i("Starting provider " + mProviderClass);
            mProvider.start();
        } catch (ProviderException e) {
            Log.w(e, "Could not start %s: give up", mProviderClass);
        }
    }

    @Override
    public void onException(ProviderException e) {
        Log.w(e, "Exception occurred in %s: try to start again", mProviderClass);
        start();
    }

    @Override
    public void onStart() {
        Log.d("%s started", mProviderClass);
    }

    @Override
    public void onStop() {
        Log.d("%s stopped", mProviderClass);
    }

    @Override
    public void add(CharSequence... messages) {
        mMessageQueueable.add(messages);
    }

    @Override
    public void addUrgent(CharSequence... messages) {
        mMessageQueueable.addUrgent(messages);
    }

    void stop() {
        if (mProvider != null) {
            mProvider.stop();
        }
    }
}
