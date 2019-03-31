/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2018 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.ticker.ticker

import org.jraf.android.ticker.BuildConfig
import org.jraf.android.ticker.app.Application
import org.jraf.android.ticker.pref.MainPrefs
import org.jraf.libticker.httpconf.Configuration
import org.jraf.libticker.httpconf.HttpConf
import org.jraf.libticker.message.BasicMessageQueue
import org.jraf.libticker.message.MessageQueue
import org.jraf.libticker.plugin.manager.PluginManager

object Ticker {
    private const val QUEUE_SIZE = 100

    private val mainPrefs by lazy {
        MainPrefs.get(Application.APPLICATION_CONTEXT)
    }

    val messageQueue: MessageQueue = BasicMessageQueue(QUEUE_SIZE)
    val pluginManager: PluginManager = PluginManager(messageQueue).apply {
        // Load plugin configuration (if any)
        if (mainPrefs.containsPluginConfiguration()) {
            managePlugins(mainPrefs.pluginConfiguration!!, false)
        }

        // Persist plugin configuration
        managedPluginsChanged.subscribe { jsonString ->
            mainPrefs.pluginConfiguration = jsonString
        }
    }

    // Http conf
    val httpConf = HttpConf(
        pluginManager,
        Configuration(
            appName = Application.APP_NAME,
            appVersion = "${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}"
        )
    ).apply {
        start()
    }
}