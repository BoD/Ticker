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
package org.jraf.android.ticker.provider.twitter

import android.content.Context
import org.jraf.android.ticker.R
import org.jraf.android.util.listeners.Listeners
import org.jraf.android.util.log.Log
import twitter4j.Paging
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.util.Collections
import java.util.Comparator
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class TwitterClient private constructor(val mContext: Context) {
    companion object {
        private val CHECK_PERIOD = TimeUnit.MINUTES.toMillis(3)
        private const val RETRIEVE_COUNT = 30

        private val STATUS_COMPARATOR = Comparator<twitter4j.Status> { o1, o2 -> o1.createdAt.compareTo(o2.createdAt) }

        fun newInstance(context: Context): TwitterClient {
            val res = TwitterClient(context.applicationContext)
            return res
        }
    }

    private var mScheduledExecutorService: ScheduledExecutorService? = null
    private val mListeners = Listeners<StatusListener>()

    fun addListener(listener: StatusListener) {
        mListeners.add(listener)
    }

    fun removeListener(listener: StatusListener) {
        mListeners.remove(listener)
    }

    fun startClient() {
        mScheduledExecutorService = Executors.newScheduledThreadPool(1)
        mScheduledExecutorService!!.scheduleAtFixedRate(CheckForNewTweetsRunnable(), 0, CHECK_PERIOD, TimeUnit.MILLISECONDS)
    }

    fun stopClient() {
        mScheduledExecutorService?.shutdownNow()
        mScheduledExecutorService = null
    }

    private val mTwitter: Twitter by lazy {
        val configurationBuilder = ConfigurationBuilder()
        configurationBuilder.setDebugEnabled(true).setOAuthConsumerKey(mContext.getString(R.string.apiKeyTwitterOauthConsumerKey))
        configurationBuilder.setOAuthConsumerSecret(mContext.getString(R.string.apiKeyTwitterOauthConsumerSecret))
        configurationBuilder.setOAuthAccessToken(mContext.getString(R.string.apiKeyTwitterOauthAccessToken))
        configurationBuilder.setOAuthAccessTokenSecret(mContext.getString(R.string.apiKeyTwitterOauthAccessTokenSecret))
        val twitterFactory = TwitterFactory(configurationBuilder.build())
        twitterFactory.instance
    }

    private inner class CheckForNewTweetsRunnable : Runnable {
        private var mPreviousStatuses: List<Status>? = null

        override fun run() {
            try {
                Log.d("Checking for new tweets")
                val statusList = mTwitter.getUserListStatuses("bod", "news", Paging(1, RETRIEVE_COUNT))
                if (statusList.isEmpty()) {
                    Log.d("No tweets")
                    return
                }
                // Sort them
                Collections.sort(statusList, STATUS_COMPARATOR)
                val previousStatuses = statusList.toList()
                // Remove the tweets from last round (if any) to get only the new ones
                if (mPreviousStatuses != null) statusList.removeAll(mPreviousStatuses!!)
                mPreviousStatuses = previousStatuses

                if (statusList.isEmpty()) {
                    // No change
                    Log.d("No new tweets")
                    return
                }

                // New tweets (or first time): dispatch to listeners
                Log.d("${statusList.size} new tweets")
                mListeners.dispatch { it.onNewStatuses(statusList) }
            } catch (e: Exception) {
                Log.w(e, "Could not retrieve tweets")
            }
        }
    }
}
