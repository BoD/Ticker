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
package org.jraf.android.ticker.app

import android.content.Context
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.multidex.MultiDexApplication
import org.jraf.android.ticker.BuildConfig
import org.jraf.android.util.log.Log

class Application : MultiDexApplication() {
    companion object {
        const val APP_NAME = "BoD Ticker"
        private const val TAG = "Ticker"
        lateinit var APPLICATION_CONTEXT: Context
    }

    override fun onCreate() {
        super.onCreate()

        APPLICATION_CONTEXT = this

        // Log
        Log.init(this, TAG, BuildConfig.DEBUG_LOGS)

        // Emoji compat
        EmojiCompat.init(
            BundledEmojiCompatConfig(this)
                .setReplaceAll(false)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() = Log.d()
                    override fun onFailed(throwable: Throwable?) =
                        Log.w(throwable, "Could not init EmojiCompat")
                })
        )
    }
}
