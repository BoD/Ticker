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
package org.jraf.android.ticker.glide

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import org.jraf.android.ticker.BuildConfig

@GlideModule
class AppGlideModule : AppGlideModule() {
    companion object {
        private const val CACHE_SIZE_B = 5 * 1024 * 1024L
        private const val CACHE_DIRECTORY_NAME = "images"
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val memorySizeCalculator = MemorySizeCalculator.Builder(context).build()

        builder
            // Disk cache
            .setDiskCache(
                InternalCacheDiskCacheFactory(
                    context,
                    CACHE_DIRECTORY_NAME,
                    CACHE_SIZE_B
                )
            )

            // Memory cache / bitmap pool
            .setMemoryCache(LruResourceCache(memorySizeCalculator.memoryCacheSize.toLong()))
            .setBitmapPool(LruBitmapPool(memorySizeCalculator.bitmapPoolSize.toLong()))

            .setDefaultRequestOptions(
                RequestOptions()
                    // Decode format
                    .format(DecodeFormat.PREFER_ARGB_8888)
            )

            // Logs
            .setLogLevel(if (BuildConfig.DEBUG_LOGS) android.util.Log.VERBOSE else android.util.Log.WARN)
    }

    override fun isManifestParsingEnabled() = false
}
