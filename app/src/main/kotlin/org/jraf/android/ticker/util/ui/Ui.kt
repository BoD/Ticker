/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2019-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.ticker.util.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

fun View.fadeOut() {
    if (visibility != View.VISIBLE) return
    animate().alpha(0F).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            visibility = View.GONE
        }
    })
}

fun View.fadeIn() {
    if (visibility != View.GONE) return
    alpha = 0F
    visibility = View.VISIBLE
    animate().alpha(1F).setListener(null)
}