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
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import ca.rmen.sunrisesunset.SunriseSunset
import org.jraf.android.ticker.R
import org.jraf.android.ticker.databinding.MainBinding
import org.jraf.android.ticker.message.MessageQueue
import org.jraf.android.ticker.pref.MainPrefs
import org.jraf.android.ticker.provider.datetimeweather.weather.LocationUtil
import org.jraf.android.ticker.provider.manager.ProviderManager
import org.jraf.android.ticker.util.emoji.EmojiUtil.replaceEmojis
import org.jraf.android.util.log.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private const val QUEUE_SIZE = 40
        private const val REQUEST_PERMISSION_LOCATION = 0
        private const val FONT_NAME = "RobotoCondensed-Regular-No-Ligatures.ttf"
        private val UPDATE_BRIGHTNESS_RATE_MS = TimeUnit.MINUTES.toMillis(1)
        private val UPDATE_TEXT_RATE_MS = TimeUnit.SECONDS.toMillis(12)
        private val TYPEWRITER_EFFECT_DELAY_MS = 33L
    }

    private lateinit var mBinding: MainBinding
    private lateinit var mTextQueue: MessageQueue
    private val mToast: Toast by lazy {
        Toast.makeText(this, "", Toast.LENGTH_SHORT)
    }
    private var mLocation: Location? = null

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
        setTickerText(getString(R.string.main_fetching))

        mBinding.root.setOnTouchListener(mAdjustOnTouchListener)
    }

    override fun onResume() {
        super.onResume()
        val hasPermissions = checkPermissions()
        if (hasPermissions) onPermissionsGranted()
    }

    private fun onPermissionsGranted() {
        thread {
            mLocation = LocationUtil.getRecentLocation(MainActivity@ this, 30, TimeUnit.SECONDS)
        }

        ProviderManager.startProviders(this, mTextQueue)

        mUpdateBrightnessAndBackgroundOpacityHandler.sendEmptyMessage(0)
        mUpdateTextHandler.sendEmptyMessage(0)
    }

    override fun onPause() {
        ProviderManager.stopProviders()
        mUpdateBrightnessAndBackgroundOpacityHandler.removeCallbacksAndMessages(null)
        mUpdateTextHandler.removeCallbacksAndMessages(null)
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
                onPermissionsGranted()
            } else {
                Log.w("Permission not granted: show a snackbar")
                Snackbar.make(mBinding.root, R.string.main_permissionNotGranted, Snackbar.LENGTH_INDEFINITE).show()
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    // endregion


    private fun adjustFontSize(tickerText: CharSequence) {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val smallSide = Math.min(rect.width(), rect.height())

        // A font size of about ~1/8 to 1/10 screen small side is a sensible value for the starting font size
        var fontSize = (smallSide / 10f).toInt()
        mBinding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())

        mBinding.txtTicker.text = tickerText;

        mBinding.txtTicker.measure(View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED)
        while (mBinding.txtTicker.measuredHeight < rect.height()) {
            fontSize += 2
            mBinding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
            mBinding.txtTicker.measure(View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED)
        }
        fontSize -= 2
        mBinding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        mBinding.txtTicker.text = null
    }

    private fun setTickerText(tickerText: CharSequence) {
        val text = tickerText.replaceEmojis(mBinding.txtTicker)

        adjustFontSize(text)

        // Change the color randomly
        val hsv = FloatArray(3)
        hsv[0] = (Math.random() * 360f).toFloat()
        hsv[1] = .75f
        hsv[2] = .75f
        val color = Color.HSVToColor(hsv)
        mBinding.txtTicker.setTextColor(color)

        for (i in 0 until text.length) {
            mBinding.txtTicker.postDelayed({
                val truncatedText = SpannableStringBuilder(text)
                truncatedText.setSpan(ForegroundColorSpan(Color.TRANSPARENT), i + 1, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                mBinding.txtTicker.text = truncatedText
            }, TYPEWRITER_EFFECT_DELAY_MS * i)
        }
    }


    //--------------------------------------------------------------------------
    // region Brightness and backgroind opacity.
    //--------------------------------------------------------------------------

    private val mAdjustOnTouchListener = View.OnTouchListener { v, event ->
        val y = event.y
        val height = v.height
        val x = event.x
        val width = v.width

        val value = Math.max(0f, 1f - y / height)

        val left = x < width / 2
        if (left) {
            // Brightness
            toast(value, R.string.main_brightness_toast)
            setBrightness(value)
        } else {
            // Background opacity
            toast(value, R.string.main_backgroundOpacity_toast)
            setBackgroundOpacity(value)
        }

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (left) {
                persistBrightness(value)
            } else {
                persistBackgroundOpacity(value)
            }
        }

        true
    }

    /**
     * Handler to update the text periodically.
     */
    private val mUpdateTextHandler = object : Handler() {
        override fun handleMessage(message: Message) {
            val newText = mTextQueue.next
            if (newText != null) setTickerText(newText)

            // Reschedule
            sendEmptyMessageDelayed(0, UPDATE_TEXT_RATE_MS)
        }
    }

    /**
     * Handler to update the brightness / background opacity periodically.
     */
    private val mUpdateBrightnessAndBackgroundOpacityHandler = object : Handler() {
        override fun handleMessage(message: Message) {
            val mainPrefs = MainPrefs.get(this@MainActivity)
            if (isDay()) {
                mainPrefs.brightnessDay?.let { setBrightness(it) }
                mainPrefs.backgroundOpacityDay?.let { setBackgroundOpacity(it) }
            } else {
                mainPrefs.brightnessNight?.let { setBrightness(it) }
                mainPrefs.backgroundOpacityNight?.let { setBackgroundOpacity(it) }
            }

            // Reschedule
            sendEmptyMessageDelayed(0, UPDATE_BRIGHTNESS_RATE_MS)
        }
    }

    private fun persistBrightness(value: Float) {
        val mainPrefs = MainPrefs.get(this)
        if (isDay()) {
            mainPrefs.putBrightnessDay(value)
        } else {
            mainPrefs.putBrightnessNight(value)
        }
    }

    private fun persistBackgroundOpacity(value: Float) {
        val mainPrefs = MainPrefs.get(this)
        if (isDay()) {
            mainPrefs.putBackgroundOpacityDay(value)
        } else {
            mainPrefs.putBackgroundOpacityNight(value)
        }
    }

    private fun isDay(): Boolean {
        val location = mLocation
        if (location == null) {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            return hour in 8..10
        } else {
            return SunriseSunset.isDay(location.latitude, location.longitude)
        }
    }

    private fun setBrightness(value: Float) {
        val layout = window.attributes
        layout.screenBrightness = if (value < 0) 0F else if (value > 1) 1F else value
        window.attributes = layout
    }

    private fun setBackgroundOpacity(value: Float) {
        val boundValue = if (value < 0) 0F else if (value > 1) 1F else value
        mBinding.backgroundOpacity.setBackgroundColor(Color.argb((255f * (1f - boundValue)).toInt(), 0, 0, 0))
    }


    private fun toast(ratio: Float, textRes: Int) {
        val text = getString(textRes, (ratio * 100).toInt())
        with(mToast) {
            setText(text)
            duration = Toast.LENGTH_SHORT
            show()
        }
    }

    // endregion
}
