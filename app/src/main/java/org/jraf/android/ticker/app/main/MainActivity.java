package org.jraf.android.ticker.app.main;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

import org.jraf.android.ticker.R;
import org.jraf.android.ticker.databinding.MainBinding;
import org.jraf.android.ticker.twitter.StatusListener;
import org.jraf.android.ticker.twitter.TwitterClient;
import org.jraf.android.util.log.Log;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;

public class MainActivity extends AppCompatActivity {
    private static final int QUEUE_SIZE = 30;

    private MainBinding mBinding;
    private float mPixelsPerSecond;
    private TwitterClient mTwitterClient;
    private TextQueue mTextQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.main);

        mBinding.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LOW_PROFILE);

        // Set the custom font
        String fontName = "FjallaOne-Regular.ttf";
        mBinding.txtTicker.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/" + fontName));

        adjustFontSizeAndSpeed();
        mTextQueue = new TextQueue(QUEUE_SIZE);
        setTickerText(getString(R.string.main_fetching));
        startScroll();
        startTwitterClient();
        startDateTime();
    }

    @Override
    protected void onDestroy() {
        stopTwitterClient();
        stopDateTime();
        super.onDestroy();
    }

    private void adjustFontSizeAndSpeed() {
        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int smallSide = Math.min(rect.width(), rect.height());
        int bigSide = Math.max(rect.width(), rect.height());

        // A font size of about ~1 to 1/2 screen small side is a sensible value
        int fontSize = (int) (smallSide / 2f);
        mBinding.txtTicker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);

        // A speed of about ~1 to 2 horizontal screens per second is a sensible value
        mPixelsPerSecond = bigSide * 1.1f;
        Log.d("mPixelsPerSecond=%s", mPixelsPerSecond);
    }

    private void setTickerText(CharSequence tickerText) {
        mBinding.txtTicker.setText(tickerText);

        // Change the text size
        ViewGroup.LayoutParams layoutParams = mBinding.txtTicker.getLayoutParams();
        mBinding.txtTicker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        layoutParams.width = mBinding.txtTicker.getMeasuredWidth();
        mBinding.txtTicker.requestLayout();
    }

    private void startTwitterClient() {
        mTwitterClient = TwitterClient.getInstance(this);
        mTwitterClient.addListener(new StatusListener() {
            @Override
            public void onNewStatuses(List<Status> statuses) {
                for (Status status : statuses) {
                    String screenName = status.getUser().getScreenName();
                    String statusText = status.getText();

                    // Remove all urls
                    for (URLEntity urlEntity : status.getURLEntities()) {
                        statusText = statusText.replaceAll(Pattern.quote(urlEntity.getURL()), "");
                    }
                    for (MediaEntity mediaEntity : status.getMediaEntities()) {
                        statusText = statusText.replaceAll(Pattern.quote(mediaEntity.getURL()), "");
                    }

                    // Add author
                    String author = "@" + screenName;
                    statusText = author + " " + statusText;
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(statusText);
                    ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourcesCompat.getColor(getResources(), R.color.tweetAuthor, null));
                    spannableStringBuilder.setSpan(foregroundColorSpan, 0, author.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    mTextQueue.add(spannableStringBuilder);
                }
            }
        });
        mTwitterClient.startClient();
    }

    private void stopTwitterClient() {
        if (mTwitterClient != null) {
            mTwitterClient.stopClient();
        }
    }


    private Runnable mDateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            String date =
                    DateUtils.formatDateTime(MainActivity.this, now, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
            String time =
                    DateUtils.formatDateTime(MainActivity.this, now, DateUtils.FORMAT_SHOW_TIME);
            mTextQueue.addUrgent(date, time);

            startDateTime();
        }
    };

    private void startDateTime() {
        mBinding.txtTicker.postDelayed(mDateTimeRunnable, TimeUnit.MINUTES.toMillis(1));
    }

    private void stopDateTime() {
        mBinding.txtTicker.removeCallbacks(mDateTimeRunnable);
    }


    private void startScroll() {
        mBinding.txtTicker.post(new Runnable() {
            @Override
            public void run() {
                int margin = mBinding.conRoot.getWidth();
                int textWidth = mBinding.txtTicker.getWidth();
                int totalWidth = textWidth + margin;
                int animationDuration = (int) (totalWidth / mPixelsPerSecond * 1000f);
                mBinding.txtTicker.setTranslationX(margin);
                mBinding.txtTicker.animate().setInterpolator(new LinearInterpolator()).translationX(-textWidth)
                        .setDuration(animationDuration).setListener(mAnimatorListener);
            }
        });
    }

    private AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            CharSequence newText = mTextQueue.getNext();
            if (newText != null) setTickerText(newText);
            int margin = mBinding.conRoot.getWidth();
            mBinding.txtTicker.setTranslationX(margin);
            startScroll();
        }
    };

}
