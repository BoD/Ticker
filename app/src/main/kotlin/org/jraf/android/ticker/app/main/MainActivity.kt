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
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import ca.rmen.sunrisesunset.SunriseSunset
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.jraf.android.ticker.R
import org.jraf.android.ticker.databinding.MainBinding
import org.jraf.android.ticker.glide.GlideApp
import org.jraf.android.ticker.pref.MainPrefs
import org.jraf.android.ticker.ticker.Ticker
import org.jraf.android.ticker.util.emoji.EmojiUtil.replaceEmojisWithImageSpans
import org.jraf.android.ticker.util.emoji.EmojiUtil.replaceEmojisWithSmiley
import org.jraf.android.ticker.util.location.IpApiClient
import org.jraf.android.util.log.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@SuppressLint("ShowToast")
class MainActivity : AppCompatActivity() {

    companion object {
        private const val FONT_NAME = "RobotoCondensed-Regular-No-Ligatures.ttf"
        private val UPDATE_BRIGHTNESS_RATE_MS = TimeUnit.MINUTES.toMillis(1)
        private val CHECK_QUEUE_RATE_MS = TimeUnit.SECONDS.toMillis(14)
        private val SHOW_IMAGE_DURATION_MS = TimeUnit.SECONDS.toMillis(8)
        private const val TYPEWRITER_EFFECT_DELAY_MS = 33L

        private const val MESSAGE_CHECK_QUEUE = 0
        private const val MESSAGE_SHOW_TEXT = 1
    }

    private lateinit var binding: MainBinding

    private val toast: Toast by lazy {
        Toast.makeText(this, "", Toast.LENGTH_SHORT)
    }
    private var location: Location? = null

    private val mainPrefs by lazy {
        MainPrefs.get(this)
    }

    @SuppressLint("InlinedApi", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
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
        binding.txtTicker.typeface = Typeface.createFromAsset(assets, "fonts/$FONT_NAME")

        Ticker.messageQueue.addUrgent(
            org.jraf.libticker.message.Message(
                text = getString(R.string.main_httpConfUrl, Ticker.httpConf.getUrl()),
                imageUri = "http://api.qrserver.com/v1/create-qr-code/?data=${Ticker.httpConf.getUrl()}"
            )
        )

        binding.root.setOnTouchListener(mAdjustOnTouchListener)
    }

    override fun onResume() {
        super.onResume()
        thread {
            location = IpApiClient().currentLocation
            Log.d("location=$location")
        }

        Ticker.pluginManager.startAllManagedPlugins()

        updateBrightnessAndBackgroundOpacityHandler.sendEmptyMessage(0)
        checkMessageQueueHandler.sendEmptyMessage(MESSAGE_CHECK_QUEUE)
    }

    override fun onPause() {
        Ticker.pluginManager.stopAllManagedPlugins()
        updateBrightnessAndBackgroundOpacityHandler.removeCallbacksAndMessages(null)
        checkMessageQueueHandler.removeCallbacksAndMessages(null)
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

        binding.txtTicker.measure(
            View.MeasureSpec.makeMeasureSpec(
                rect.width(),
                View.MeasureSpec.AT_MOST
            ), View.MeasureSpec.UNSPECIFIED
        )
        while (binding.txtTicker.measuredHeight < rect.height()) {
            fontSize += 2
            binding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
            binding.txtTicker.measure(
                View.MeasureSpec.makeMeasureSpec(
                    rect.width(),
                    View.MeasureSpec.AT_MOST
                ), View.MeasureSpec.UNSPECIFIED
            )
        }
        fontSize -= 2
        binding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        binding.txtTicker.text = null
    }

    private fun setTickerText(text: String) {
        if (binding.imgImage.alpha > 0F) {
            binding.imgImage.animate().alpha(0F)
        }

        @Suppress("DEPRECATION")
        val formattedText = Html.fromHtml(text)

        // Adjust the font size to fit the screen.
        // Before we do this, we replace emojis with '😀' which is a wide character.
        // This allows for the ImageSpan sizes of the replaced emojis to be accounted for.
        adjustFontSize(formattedText.replaceEmojisWithSmiley())

        val textWithSpans: Spannable = formattedText.replaceEmojisWithImageSpans(binding.txtTicker)

        // Change the color randomly
        val hsv = FloatArray(3)
        hsv[0] = (Math.random() * 360f).toFloat()
        hsv[1] = .75f
        hsv[2] = .75f
        val color = Color.HSVToColor(hsv)
        binding.txtTicker.setTextColor(color)

        for (i in 0 until textWithSpans.length) {
            binding.txtTicker.postDelayed({
                val truncatedText = SpannableStringBuilder(textWithSpans)
                truncatedText.setSpan(
                    ForegroundColorSpan(Color.TRANSPARENT),
                    i + 1,
                    textWithSpans.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                binding.txtTicker.text = truncatedText
            }, TYPEWRITER_EFFECT_DELAY_MS * i)
        }
    }


    //--------------------------------------------------------------------------
    // region Brightness and background opacity.
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
     * Handler to check the message queue periodically, and show the text and/or image.
     */
    private val checkMessageQueueHandler =
        @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    MESSAGE_CHECK_QUEUE -> {
                        val newMessage = Ticker.messageQueue.next
                        if (newMessage == null) {
                            // Check again later
                            sendEmptyMessageDelayed(MESSAGE_CHECK_QUEUE, CHECK_QUEUE_RATE_MS)
                            return
                        }

                        if (newMessage.imageUri != null) {
                            // There is an image: show it now, and show the text later
                            showImage(newMessage.imageUri!!)
                            sendMessageDelayed(
                                Message.obtain(this, MESSAGE_SHOW_TEXT, newMessage.textFormatted),
                                SHOW_IMAGE_DURATION_MS
                            )
                        } else {
                            // Just the text: show it now
                            setTickerText(newMessage.textFormatted)

                            // Reschedule
                            sendEmptyMessageDelayed(MESSAGE_CHECK_QUEUE, CHECK_QUEUE_RATE_MS)
                        }
                    }

                    MESSAGE_SHOW_TEXT -> {
                        setTickerText(message.obj as String)

                        // Reschedule
                        sendEmptyMessageDelayed(MESSAGE_CHECK_QUEUE, CHECK_QUEUE_RATE_MS)
                    }
                }
            }
        }

    private fun showImage(imageUri: String) {
        Log.d("imageUri=$imageUri")
        GlideApp.with(this)
            .load(imageUri)
            .fitCenter()
            .transition(DrawableTransitionOptions.withCrossFade())
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.txtTicker.text = null
                    binding.imgImage.alpha = 1F
                    binding.imgImage.setImageDrawable(null)
                    return false
                }
            })
            .into(binding.imgImage)
    }

    /**
     * Handler to update the brightness / background opacity periodically.
     */
    private val updateBrightnessAndBackgroundOpacityHandler =
        @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(message: Message) {
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
        if (isDay()) {
            mainPrefs.putBrightnessDay(value)
        } else {
            mainPrefs.putBrightnessNight(value)
        }
    }

    private fun persistBackgroundOpacity(value: Float) {
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
        val sanitizedValue = value.coerceIn(0F, 1F)
        val brightnessValue = (sanitizedValue - .5F).coerceAtLeast(0F) * 2F
        val layout = window.attributes
        layout.screenBrightness = brightnessValue
        window.attributes = layout
        val alphaValue = sanitizedValue.coerceAtMost(.5F) * 2F
        binding.foregroundOpacity.setBackgroundColor(
            Color.argb(
                (255f * (1f - alphaValue)).toInt(),
                0,
                0,
                0
            )
        )
    }

    private fun setBackgroundOpacity(value: Float) {
        val sanitizedValue = value.coerceIn(0F, 1F)
        binding.backgroundOpacity.setBackgroundColor(
            Color.argb(
                (255f * (1f - sanitizedValue)).toInt(),
                0,
                0,
                0
            )
        )
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
