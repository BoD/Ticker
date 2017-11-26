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
package org.jraf.android.ticker.util.emoji

import android.support.v4.content.res.ResourcesCompat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.widget.TextView
import org.jraf.android.ticker.R

object EmojiUtil {
    private val DRAWABLE_MAP = mapOf(
            "☀" to R.drawable.emoji_u2600,
            "☁" to R.drawable.emoji_u2601,
            "\uD83C\uDF2B" to R.drawable.emoji_u1f32b,
            "\u26F8" to R.drawable.emoji_u26f8,
            "☂" to R.drawable.emoji_u2602,
            "☃" to R.drawable.emoji_u2603,
            "\u26C8" to R.drawable.emoji_u26c8,
            "\uD83D\uDCA8" to R.drawable.emoji_u1f4a8,
            "\uD83C\uDF19" to R.drawable.emoji_u1f319,
            "\uD83C\uDF21" to R.drawable.emoji_u1f321,
            "\uD83C\uDF24" to R.drawable.emoji_u1f324)

    private const val SIZE_FACTOR = .65

    fun CharSequence.replaceEmojisWithImageSpans(textView: TextView): Spannable {
        val res = SpannableStringBuilder(this)

        for ((key, drawableResId) in DRAWABLE_MAP) {
            var index = TextUtils.indexOf(this, key, 0)
            while (index != -1) {
                val icon = ResourcesCompat.getDrawable(textView.resources, drawableResId, null)!!
                val height = textView.lineHeight
                icon.setBounds(0, 0, (height * SIZE_FACTOR).toInt(), (height * SIZE_FACTOR).toInt())
                val imageSpan = ImageSpan(icon, DynamicDrawableSpan.ALIGN_BASELINE)
                res.setSpan(imageSpan, index, index + key.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

                index = TextUtils.indexOf(this, key, index + 1)
            }
        }

        return res
    }

    fun CharSequence.replaceEmojisWithSmiley(): CharSequence {
        var res = this
        for (key in DRAWABLE_MAP.keys) {
            res = res.toString().replace(key, "\uD83D\uDE00")
        }
        return res
    }

}
