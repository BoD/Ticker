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

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.text.Html
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
import org.jraf.android.ticker.pref.MainPrefs
import org.jraf.android.ticker.util.emoji.EmojiUtil.replaceEmojisWithImageSpans
import org.jraf.android.ticker.util.emoji.EmojiUtil.replaceEmojisWithSmiley
import org.jraf.android.ticker.util.location.IpApiClient
import org.jraf.android.util.log.Log
import org.jraf.libticker.message.BasicMessageQueue
import org.jraf.libticker.message.MessageQueue
import org.jraf.libticker.plugin.api.PluginConfiguration
import org.jraf.libticker.plugin.manager.PluginManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@SuppressLint("ShowToast")
class MainActivity : AppCompatActivity() {

    companion object {
        private const val QUEUE_SIZE = 50
        private const val FONT_NAME = "RobotoCondensed-Regular-No-Ligatures.ttf"
        private val UPDATE_BRIGHTNESS_RATE_MS = TimeUnit.MINUTES.toMillis(1)
        private val UPDATE_TEXT_RATE_MS = TimeUnit.SECONDS.toMillis(14)
        private const val TYPEWRITER_EFFECT_DELAY_MS = 33L
    }

    private lateinit var binding: MainBinding
    private lateinit var messageQueue: MessageQueue
    private lateinit var pluginManager: PluginManager
    private val toast: Toast by lazy {
        Toast.makeText(this, "", Toast.LENGTH_SHORT)
    }
    private var location: Location? = null

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.main)

        binding.root.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LOW_PROFILE)

        // Set the custom font
        binding.txtTicker.typeface = Typeface.createFromAsset(assets, "fonts/" + FONT_NAME)

        messageQueue = BasicMessageQueue(QUEUE_SIZE)
        pluginManager = PluginManager(messageQueue)
            .addPlugins(
                "org.jraf.libticker.plugin.datetime.DateTimePlugin" to null,
                "org.jraf.libticker.plugin.frc.FrcPlugin" to null,
                "org.jraf.libticker.plugin.weather.WeatherPlugin" to PluginConfiguration().apply {
                    put("apiKey", getString(R.string.apiKeyForecastIo))
                },
                "org.jraf.libticker.plugin.btc.BtcPlugin" to null,
                "org.jraf.libticker.plugin.twitter.TwitterPlugin" to PluginConfiguration().apply {
                    put(
                        "oAuthConsumerKey",
                        getString(R.string.apiKeyTwitterOauthConsumerKey)
                    )
                    put(
                        "oAuthConsumerSecret",
                        getString(R.string.apiKeyTwitterOauthConsumerSecret)
                    )
                    put(
                        "oAuthAccessToken",
                        getString(R.string.apiKeyTwitterOauthAccessToken)
                    )
                    put(
                        "oAuthAccessTokenSecret",
                        getString(R.string.apiKeyTwitterOauthAccessTokenSecret)
                    )
                }
            )


        setTickerText(getString(R.string.main_fetching))

        binding.root.setOnTouchListener(mAdjustOnTouchListener)
    }

    override fun onResume() {
        super.onResume()
        thread {
            location = IpApiClient().currentLocation
            Log.d("location=$location")
        }

        pluginManager.start()

        updateBrightnessAndBackgroundOpacityHandler.sendEmptyMessage(0)
        updateTextHandler.sendEmptyMessage(0)
    }

    override fun onPause() {
        pluginManager.stop()
        updateBrightnessAndBackgroundOpacityHandler.removeCallbacksAndMessages(null)
        updateTextHandler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    private fun adjustFontSize(tickerText: CharSequence) {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val smallSide = Math.min(rect.width(), rect.height())

        // A font size of about ~1/8 to 1/10 screen small side is a sensible value for the starting font size
        var fontSize = (smallSide / 10f).toInt()
        binding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())

        binding.txtTicker.text = tickerText

        binding.txtTicker.measure(View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED)
        while (binding.txtTicker.measuredHeight < rect.height()) {
            fontSize += 2
            binding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
            binding.txtTicker.measure(View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.AT_MOST), View.MeasureSpec.UNSPECIFIED)
        }
        fontSize -= 2
        binding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        binding.txtTicker.text = null
    }

    private fun setTickerText(text: String) {
        val formattedText = Html.fromHtml(text)

        // Adjust the font size to fit the screen.
        // Before we do this, we replace emojis with '😀' which is a wide character.
        // This allows for the ImageSpan sizes of the replaced emojis to be accounted for.
        adjustFontSize(formattedText.replaceEmojisWithSmiley())

        val text = formattedText.replaceEmojisWithImageSpans(binding.txtTicker)

        // Change the color randomly
        val hsv = FloatArray(3)
        hsv[0] = (Math.random() * 360f).toFloat()
        hsv[1] = .75f
        hsv[2] = .75f
        val color = Color.HSVToColor(hsv)
        binding.txtTicker.setTextColor(color)

        for (i in 0 until text.length) {
            binding.txtTicker.postDelayed({
                val truncatedText = SpannableStringBuilder(text)
                truncatedText.setSpan(ForegroundColorSpan(Color.TRANSPARENT), i + 1, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                binding.txtTicker.text = truncatedText
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
    private val updateTextHandler =
        @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(message: Message) {
                val newText = messageQueue.next
                if (newText != null) setTickerText(newText)

                // Reschedule
                sendEmptyMessageDelayed(0, UPDATE_TEXT_RATE_MS)
            }
        }

    /**
     * Handler to update the brightness / background opacity periodically.
     */
    private val updateBrightnessAndBackgroundOpacityHandler =
        @SuppressLint("HandlerLeak")
        object : Handler() {
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
        val location = location
        return if (location == null) {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            hour in 8..10
        } else {
            SunriseSunset.isDay(location.latitude, location.longitude)
        }
    }

    private fun setBrightness(value: Float) {
        val layout = window.attributes
        layout.screenBrightness = if (value < 0) 0F else if (value > 1) 1F else value
        window.attributes = layout
    }

    private fun setBackgroundOpacity(value: Float) {
        val boundValue = if (value < 0) 0F else if (value > 1) 1F else value
        binding.backgroundOpacity.setBackgroundColor(Color.argb((255f * (1f - boundValue)).toInt(), 0, 0, 0))
    }


    private fun toast(ratio: Float, textRes: Int) {
        val text = getString(textRes, (ratio * 100).toInt())
        with(toast) {
            setText(text)
            duration = Toast.LENGTH_SHORT
            show()
        }
    }

    // endregion
}
