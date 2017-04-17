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
import android.support.v4.content.res.ResourcesCompat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import org.jraf.android.ticker.R
import org.jraf.android.ticker.provider.Provider
import org.jraf.android.ticker.provider.ProviderException
import org.jraf.android.ticker.provider.manager.ProviderManagerCallbacks
import twitter4j.Status
import java.util.regex.Pattern

class TwitterProvider : Provider {
    private lateinit var mContext: Context
    private lateinit var mCallbacks: ProviderManagerCallbacks
    private var mTwitterClient: TwitterClient? = null

    @Throws(ProviderException::class)
    override fun init(context: Context, callbacks: ProviderManagerCallbacks) {
        mContext = context
        mCallbacks = callbacks
    }

    @Throws(ProviderException::class)
    override fun start() {
        mTwitterClient = TwitterClient.newInstance(mContext)
        mTwitterClient!!.addListener(object : StatusListener {
            override fun onNewStatuses(statuses: List<Status>) {
                for (status in statuses) {
                    val screenName = status.user.screenName
                    var statusText = status.text

                    // Remove all urls
                    for (urlEntity in status.urlEntities) {
                        statusText = statusText.replace(Pattern.quote(urlEntity.url).toRegex(), "")
                    }
                    for (mediaEntity in status.mediaEntities) {
                        statusText = statusText.replace(Pattern.quote(mediaEntity.url).toRegex(), "")
                    }

                    // Remove trailing ':' (usually left over after removing urls)
                    statusText = statusText.trim { it <= ' ' }
                    if (statusText.endsWith(":")) {
                        statusText = statusText.substring(0, statusText.length - 1)
                    }

                    // Add author
                    val author = "@" + screenName
                    statusText = author + " " + statusText
                    val spannableStringBuilder = SpannableStringBuilder(statusText)
                    val foregroundColorSpan = ForegroundColorSpan(ResourcesCompat.getColor(mContext.resources, R.color.tweetAuthor, null))
                    spannableStringBuilder.setSpan(foregroundColorSpan, 0, author.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    val sizeSpan = RelativeSizeSpan(.6F)
                    spannableStringBuilder.setSpan(sizeSpan, 0, author.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    mCallbacks.add(spannableStringBuilder)
                }
            }
        })
        mTwitterClient!!.startClient()
        mCallbacks.onStart()
    }

    override fun stop() {
        mTwitterClient?.stopClient()
        mCallbacks.onStop()
    }
}
