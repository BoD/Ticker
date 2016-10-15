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
package org.jraf.android.ticker.message

import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Arrays

class MessageQueue(private val mSize: Int) : MessageQueueable {
    private val mList: ArrayList<CharSequence> = ArrayList(mSize * 2)
    private val mUrgentQueue: ArrayDeque<CharSequence> = ArrayDeque(mSize * 2)
    private var mCurrentIndex = 0

    val next: CharSequence?
        @Synchronized get() {
            // Try the urgent queue first
            if (!mUrgentQueue.isEmpty()) {
                return mUrgentQueue.pop()
            }

            // Try the normal list
            if (mList.isEmpty()) return null
            val res = mList[mCurrentIndex]
            mCurrentIndex = (mCurrentIndex + 1) % mList.size
            return res
        }


    @Synchronized override fun add(vararg messages: CharSequence) {
        mList.addAll(Arrays.asList(*messages))

        // Discard old items if any
        var elementsToDiscard = 0
        if (mList.size > mSize) {
            elementsToDiscard = mList.size - mSize
            mList.subList(0, elementsToDiscard).clear()
        }

        // Shift the index if elements were discarded
        mCurrentIndex -= elementsToDiscard
        if (mCurrentIndex < 0) mCurrentIndex = 0
    }

    operator fun plusAssign(message: CharSequence) {
        add(message)
    }


    @Synchronized override fun addUrgent(vararg messages: CharSequence) {
        mUrgentQueue.addAll(Arrays.asList(*messages))
    }

    operator fun timesAssign(message: CharSequence) {
        add(message)
    }
}