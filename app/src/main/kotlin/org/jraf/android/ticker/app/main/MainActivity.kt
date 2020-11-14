/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.postDelayed
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
import org.jraf.android.ticker.util.ui.fadeIn
import org.jraf.android.ticker.util.ui.fadeOut
import org.jraf.android.util.log.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.min

typealias TickerMessage = org.jraf.libticker.message.Message

@SuppressLint("ShowToast")
class MainActivity : AppCompatActivity() {

    companion object {
        private const val FONT_NAME = "RobotoCondensed-Regular-No-Ligatures.ttf"
        private val UPDATE_BRIGHTNESS_RATE_MS = TimeUnit.MINUTES.toMillis(1)

        private val CHECK_QUEUE_RATE_MS = TimeUnit.SECONDS.toMillis(14)

        private val SHOW_IMAGE_DURATION_MEDIUM_MS = TimeUnit.SECONDS.toMillis(10)
        private val SHOW_IMAGE_DURATION_LONG_MS = TimeUnit.SECONDS.toMillis(28)
        private val SHOW_MINI_TICKER_DELAY_MS = TimeUnit.SECONDS.toMillis(2)
        private val HIDE_MINI_TICKER_DELAY_MS = TimeUnit.SECONDS.toMillis(10)
        private const val TYPEWRITER_EFFECT_DURATION_MS = 1800L

        private const val MESSAGE_CHECK_QUEUE = 0
        private const val MESSAGE_SHOW_TEXT = 1
        private const val MESSAGE_HIDE_MINI_TICKER = 2
    }

    private lateinit var binding: MainBinding

    private var location: Location? = null

    private val mainPrefs by lazy {
        MainPrefs(this)
    }

    @SuppressLint("InlinedApi", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = DataBindingUtil.setContentView(this, R.layout.main)

        binding.root.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LOW_PROFILE)

        // Set the custom font
        val typeface = Typeface.createFromAsset(assets, "fonts/$FONT_NAME")
        binding.txtTicker.typeface = typeface
        binding.txtMiniTicker.typeface = typeface

        Ticker.messageQueue.addUrgent(
            TickerMessage(
                text = getString(R.string.main_httpConfUrl, Ticker.httpConf.getUrl()),
                imageUri = "https://api.qrserver.com/v1/create-qr-code/?data=${Ticker.httpConf.getUrl()}"
            )
        )

        binding.foregroundOpacity.setOnTouchListener(
            adjustBrightnessAndBackgroundOpacityOnTouchListener
        )

        binding.webTicker.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.imgImage.fadeOut()
                binding.webTicker.fadeIn()
                binding.txtTicker.fadeOut()
                binding.txtMiniTicker.fadeOut()
            }
        }
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

        // Inform the ticker of the display size
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        Ticker.pluginManager.globalConfiguration.run {
            put("displayWidth", displayMetrics.widthPixels)
            put("displayHeight", displayMetrics.heightPixels)
        }
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
        val smallSide = min(rect.width(), rect.height())

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

    private fun showMessageText(message: TickerMessage) {
        val html = message.html
        if (html != null) {
            setTickerHtml(html)
        } else {
            setTickerText(message.textFormatted)
        }
    }

    private fun setTickerText(text: String) {
        binding.imgImage.fadeOut()
        binding.webTicker.fadeOut()
        binding.txtMiniTicker.fadeOut()
        binding.txtTicker.fadeIn()

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

        val delay =
            if (textWithSpans.length <= 10) 0 else TYPEWRITER_EFFECT_DURATION_MS / textWithSpans.length
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
            }, delay * i)
        }
    }

    private fun setTickerHtml(html: String) {
        binding.webTicker.loadData(
            Base64.encodeToString(html.toByteArray(), Base64.NO_PADDING),
            "text/html; charset=utf-8",
            "base64"
        )
    }

    /**
     * Handler to check the message queue periodically, and show the text and/or image.
     */
    private val checkMessageQueueHandler =
        @SuppressLint("HandlerLeak")
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    MESSAGE_CHECK_QUEUE -> {
                        val newMessage = Ticker.messageQueue.getNext()
                        if (newMessage == null) {
                            // Check again later
                            sendEmptyMessageDelayed(MESSAGE_CHECK_QUEUE, CHECK_QUEUE_RATE_MS)
                            return
                        }

                        if (newMessage.imageUri != null) {
                            // There is an image: show it now, and show the text later (unless hint to show it on top of image is present)
                            val isCropAllowed = newMessage.hints["image.cropAllowed"] == "true"
                            val isLongDisplayDuration = newMessage.hints["image.displayDuration"] == "long"
                            val showTextOnTopOfImage = newMessage.hints["text.showOnTopOfImage"] == "true"
                            val delay = if (isLongDisplayDuration) SHOW_IMAGE_DURATION_LONG_MS else SHOW_IMAGE_DURATION_MEDIUM_MS
                            if (showTextOnTopOfImage) {
                                showImage(newMessage.imageUri!!, isCropAllowed, newMessage.text)
                                // Reschedule
                                sendEmptyMessageDelayed(MESSAGE_CHECK_QUEUE, delay)
                            } else {
                                showImage(newMessage.imageUri!!, isCropAllowed, null)
                                // Show the text later
                                sendMessageDelayed(
                                    Message.obtain(this, MESSAGE_SHOW_TEXT, newMessage),
                                    delay
                                )
                            }
                        } else {
                            // Just the text: show it now
                            showMessageText(newMessage)

                            // Reschedule
                            sendEmptyMessageDelayed(MESSAGE_CHECK_QUEUE, CHECK_QUEUE_RATE_MS)
                        }
                    }

                    MESSAGE_SHOW_TEXT -> {
                        showMessageText(message.obj as TickerMessage)

                        // Reschedule
                        sendEmptyMessageDelayed(MESSAGE_CHECK_QUEUE, CHECK_QUEUE_RATE_MS)
                    }

                    MESSAGE_HIDE_MINI_TICKER -> binding.txtMiniTicker.fadeOut()
                }
            }
        }

    private fun showImage(imageUri: String, cropAllowed: Boolean, text: String?) {
        Log.d("imageUri=$imageUri cropAllowed=$cropAllowed text=$text")
        var glideRequest = GlideApp.with(this).load(imageUri)
        glideRequest = if (cropAllowed) {
            glideRequest.centerCrop()
        } else {
            glideRequest.fitCenter()
        }
        glideRequest
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
                    binding.txtTicker.fadeOut()
                    binding.webTicker.fadeOut()
                    binding.imgImage.visibility = View.VISIBLE
                    binding.imgImage.alpha = 1F
                    binding.imgImage.setImageDrawable(null)

                    if (text == null) {
                        binding.txtMiniTicker.visibility = View.GONE
                    } else {
                        binding.txtMiniTicker.postDelayed(SHOW_MINI_TICKER_DELAY_MS) {
                            binding.txtMiniTicker.fadeIn()
                            binding.txtMiniTicker.text = text
                            checkMessageQueueHandler.sendEmptyMessageDelayed(MESSAGE_HIDE_MINI_TICKER, HIDE_MINI_TICKER_DELAY_MS)
                        }
                    }
                    return false
                }
            })
            .into(binding.imgImage)
    }


    //--------------------------------------------------------------------------
    // region Brightness and background opacity.
    //--------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private val adjustBrightnessAndBackgroundOpacityOnTouchListener =
        View.OnTouchListener { v, event ->
            var y = event.y
            val height = v.height.toFloat()
            val safeZone = 1.8F
            y = y * safeZone - (height * safeZone - height) / 2F
            val x = event.x
            val width = v.width

            val ratio = (1F - y / height).coerceIn(0F..1F)

            val left = x < width / 2
            if (left) {
                // Brightness
                showBrightnessPanel(ratio)
                setBrightness(ratio)
            } else {
                // Background opacity
                showBackgroundOpacityPanel(ratio)
                setBackgroundOpacity(ratio)
            }

            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                if (left) {
                    persistBrightness(ratio)
                } else {
                    persistBackgroundOpacity(ratio)
                }
                binding.txtBrightness.visibility = View.GONE
                binding.txtBackgroundOpacity.visibility = View.GONE
            }

            true
        }

    private fun showBrightnessPanel(ratio: Float) {
        with(binding.txtBrightness) {
            visibility = View.VISIBLE
            setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.infoPanelBackground
                )
            )
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.infoPanelSelected
                )
            )
            text = getString(R.string.main_brightness, (ratio * 100).toInt())
        }

        with(binding.txtBackgroundOpacity) {
            visibility = View.VISIBLE
            setBackgroundColor(0)
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.infoPanelDefault
                )
            )
            text = getString(
                R.string.main_backgroundOpacity,
                ((if (isDay()) mainPrefs.backgroundOpacityDay else mainPrefs.backgroundOpacityNight) * 100).toInt()
            )
        }
    }

    private fun showBackgroundOpacityPanel(ratio: Float) {
        with(binding.txtBackgroundOpacity) {
            visibility = View.VISIBLE
            setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.infoPanelBackground
                )
            )
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.infoPanelSelected
                )
            )
            text = getString(R.string.main_backgroundOpacity, (ratio * 100).toInt())
        }

        with(binding.txtBrightness) {
            visibility = View.VISIBLE
            setBackgroundColor(0)
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.infoPanelDefault
                )
            )
            text = getString(
                R.string.main_brightness,
                ((if (isDay()) mainPrefs.brightnessDay else mainPrefs.brightnessNight) * 100).toInt()
            )
        }
    }

    /**
     * Handler to update the brightness / background opacity periodically.
     */
    private val updateBrightnessAndBackgroundOpacityHandler =
        @SuppressLint("HandlerLeak")
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                if (isDay()) {
                    setBrightness(mainPrefs.brightnessDay)
                    setBackgroundOpacity(mainPrefs.backgroundOpacityDay)
                } else {
                    setBrightness(mainPrefs.brightnessNight)
                    setBackgroundOpacity(mainPrefs.backgroundOpacityNight)
                }

                // Reschedule
                sendEmptyMessageDelayed(0, UPDATE_BRIGHTNESS_RATE_MS)
            }
        }

    private fun persistBrightness(value: Float) {
        if (isDay()) {
            mainPrefs.brightnessDay = value
        } else {
            mainPrefs.brightnessNight = value
        }
    }

    private fun persistBackgroundOpacity(value: Float) {
        if (isDay()) {
            mainPrefs.backgroundOpacityDay = value
        } else {
            mainPrefs.backgroundOpacityNight = value
        }
    }

    private fun isDay(): Boolean {
        val location = location
        return if (location == null) {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            hour in 8..22
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

    // endregion
}
