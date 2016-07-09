package org.jraf.android.ticker.app.main;

import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.widget.TextView;

import org.jraf.android.ticker.R;

public class EmojiUtil {
    public static Spannable replaceEmojis(CharSequence src, TextView textView) {
        SpannableStringBuilder res = new SpannableStringBuilder(src);
        int len = src.length();
        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);
            switch (c) {
                case '☀':
                    Drawable icon = ResourcesCompat.getDrawable(textView.getResources(), R.drawable.sun, null);
                    assert icon != null;
                    icon.setBounds(0, 0, textView.getLineHeight(), textView.getLineHeight());
                    ImageSpan imageSpan = new ImageSpan(icon, DynamicDrawableSpan.ALIGN_BOTTOM);
                    res.setSpan(imageSpan, i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    break;
            }
        }

        return res;
    }
}
