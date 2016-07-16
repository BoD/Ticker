package org.jraf.android.ticker.app.main;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

import org.jraf.android.ticker.R;
import org.jraf.android.ticker.databinding.MainBinding;
import org.jraf.android.ticker.message.MessageQueue;
import org.jraf.android.ticker.provider.manager.ProviderManager;
import org.jraf.android.ticker.util.emoji.EmojiUtil;
import org.jraf.android.util.log.Log;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int QUEUE_SIZE = 30;
    private static final int REQUEST_PERMISSION_LOCATION = 0;

    private MainBinding mBinding;
    private float mPixelsPerSecond;
    private MessageQueue mTextQueue;

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
        mTextQueue = new MessageQueue(QUEUE_SIZE);
        setTickerText(getString(R.string.main_fetching));

        startScroll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean hasPermissions = checkPermissions();
        if (hasPermissions) startListeners();
    }

    private void startListeners() {
        ProviderManager.get().startListeners(this, mTextQueue);
    }

    @Override
    protected void onPause() {
        ProviderManager.get().stopListeners();
        super.onPause();
    }


    //--------------------------------------------------------------------------
    // region Permissions.
    //--------------------------------------------------------------------------

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    startListeners();
                } else {
                    Log.w("Permission not granted: show a snackbar");
                    Snackbar.make(mBinding.getRoot(), R.string.main_permissionNotGranted, Snackbar.LENGTH_INDEFINITE).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    // endregion


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
        tickerText = EmojiUtil.replaceEmojis(tickerText, mBinding.txtTicker);
        mBinding.txtTicker.setText(tickerText);

        // Change the text size
        ViewGroup.LayoutParams layoutParams = mBinding.txtTicker.getLayoutParams();
        mBinding.txtTicker.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        layoutParams.width = mBinding.txtTicker.getMeasuredWidth();
        mBinding.txtTicker.requestLayout();
    }

    private void startScroll() {
        mBinding.txtTicker.post(new Runnable() {
            @Override
            public void run() {
                int margin = mBinding.getRoot().getWidth();
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
            int margin = mBinding.getRoot().getWidth();
            mBinding.txtTicker.setTranslationX(margin);
            startScroll();
        }
    };

}
