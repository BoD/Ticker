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
package org.jraf.android.ticker.app.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import org.jraf.android.ticker.R
import org.jraf.android.ticker.databinding.MainBinding
import org.jraf.android.ticker.message.MessageQueue
import org.jraf.android.ticker.provider.manager.ProviderManager
import org.jraf.android.ticker.util.emoji.EmojiUtil.replaceEmojis
import org.jraf.android.util.log.Log

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private const val QUEUE_SIZE = 40
        private const val REQUEST_PERMISSION_LOCATION = 0
        private const val FONT_NAME = "FjallaOne-Regular.ttf"
    }

    private lateinit var mBinding: MainBinding
    private var mPixelsPerSecond: Float = 0F
    private lateinit var mTextQueue: MessageQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        mBinding = DataBindingUtil.setContentView<MainBinding>(this, R.layout.main)

        mBinding.root.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LOW_PROFILE)

        // Set the custom font
        mBinding.txtTicker.typeface = Typeface.createFromAsset(assets, "fonts/" + FONT_NAME)

        mTextQueue = MessageQueue(QUEUE_SIZE)
        adjustFontSizeAndSpeed()
        setTickerText(getString(R.string.main_fetching))

        startScroll()
    }

    override fun onResume() {
        super.onResume()
        val hasPermissions = checkPermissions()
        if (hasPermissions) startProviders()
    }

    private fun startProviders() {
        ProviderManager.startProviders(this, mTextQueue)
    }

    override fun onPause() {
        ProviderManager.stopProviders()
        super.onPause()
    }


    //--------------------------------------------------------------------------
    // region Permissions.
    //--------------------------------------------------------------------------

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION -> if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                startProviders()
            } else {
                Log.w("Permission not granted: show a snackbar")
                Snackbar.make(mBinding.root, R.string.main_permissionNotGranted, Snackbar.LENGTH_INDEFINITE).show()
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // endregion


    private fun adjustFontSizeAndSpeed() {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val smallSide = Math.min(rect.width(), rect.height())
        val bigSide = Math.max(rect.width(), rect.height())

        // A font size of about ~1 to 1/2 screen small side is a sensible value
        val fontSize = (smallSide / 2f).toInt()
        mBinding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())

        // A speed of about ~1 to 2 horizontal screens per second is a sensible value
        mPixelsPerSecond = bigSide * 1.1f
        Log.d("mPixelsPerSecond=%s", mPixelsPerSecond)
    }

    private fun setTickerText(tickerText: CharSequence) {
        mBinding.txtTicker.text = tickerText.replaceEmojis(mBinding.txtTicker)

        // Change the color randomly
        val hsv = FloatArray(3)
        hsv[0] = (Math.random() * 360f).toFloat()
        hsv[1] = .75f
        hsv[2] = .75f
        val color = Color.HSVToColor(hsv)
        mBinding.txtTicker.setTextColor(color)

        // Change the text size
        val layoutParams = mBinding.txtTicker.layoutParams
        mBinding.txtTicker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        layoutParams.width = mBinding.txtTicker.measuredWidth
        mBinding.txtTicker.requestLayout()
    }

    private fun startScroll() {
        mBinding.txtTicker.post {
            val margin = mBinding.root.width
            val textWidth = mBinding.txtTicker.width
            val totalWidth = textWidth + margin
            val animationDuration = (totalWidth / mPixelsPerSecond * 1000f).toInt()
            mBinding.txtTicker.translationX = margin.toFloat()
            mBinding.txtTicker.animate().setInterpolator(LinearInterpolator()).translationX((-textWidth).toFloat()).setDuration(animationDuration.toLong()).setListener(mAnimatorListener)
        }
    }

    private val mAnimatorListener = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            val newText = mTextQueue.next
            if (newText != null) setTickerText(newText)
            val margin = mBinding.root.width
            mBinding.txtTicker.translationX = margin.toFloat()
            startScroll()
        }
    }
}
