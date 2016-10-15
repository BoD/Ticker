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
import org.jraf.android.ticker.provider.Provider
import org.jraf.android.ticker.provider.ProviderException
import org.jraf.android.util.log.Log

internal class ProviderEntry(context: Context,
                             private val mProviderClass: Class<out Provider>,
                             private val mMessageQueueable: MessageQueueable) : ProviderManagerCallbacks {
    private val mContext: Context = context.applicationContext
    private var mProvider: Provider? = null

    fun start() {
        val provider = mProviderClass.newInstance()
        try {
            Log.i("Initializing provider " + mProviderClass)
            provider.init(mContext, this)
        } catch (e: ProviderException) {
            Log.w(e, "Could not init %s: give up", mProviderClass)
            return
        }

        try {
            Log.i("Starting provider " + mProviderClass)
            provider.start()
        } catch (e: ProviderException) {
            Log.w(e, "Could not start %s: give up", mProviderClass)
        }

        mProvider = provider
    }

    override fun onException(e: ProviderException) {
        Log.w(e, "Exception occurred in %s: try to start again", mProviderClass)
        start()
    }

    override fun onStart() {
        Log.d("%s started", mProviderClass)
    }

    override fun onStop() {
        Log.d("%s stopped", mProviderClass)
    }

    override fun add(vararg messages: CharSequence) {
        mMessageQueueable.add(*messages)
    }

    override fun addUrgent(vararg messages: CharSequence) {
        mMessageQueueable.addUrgent(*messages)
    }

    fun stop() {
        mProvider?.stop()
    }
}
